package com.athaydes.spockframework.report.vivid

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassCodeVisitorSupport
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ConstructorNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.ModuleNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.PropertyNode
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilationFailedException
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.SourceUnit
import org.spockframework.compiler.AstUtil
import org.spockframework.compiler.SourceLookup
import org.spockframework.util.inspector.AstInspector
import org.spockframework.util.inspector.AstInspectorException
import org.spockframework.util.inspector.Inspect

import java.security.CodeSource

/**
 * Based on org.spockframework.util.inspector.AstInspector by Peter Niederwieser
 */
class VividAstInspector extends AstInspector {

    private static final String EXPRESSION_MARKER_PREFIX = "inspect_"

    private CompilePhase compilePhase = CompilePhase.CONVERSION
    private boolean throwOnNodeNotFound = true
    private final MyClassLoader classLoader
    private final MyVisitor visitor = new MyVisitor()

    private ModuleNode module
    private final Map<String, AnnotatedNode> markedNodes = new HashMap<String, AnnotatedNode>()
    private final Map<String, ClassNode> classes = new HashMap<String, ClassNode>()
    private final Map<String, FieldNode> fields = new HashMap<String, FieldNode>()
    private final Map<String, PropertyNode> properties = new HashMap<String, PropertyNode>()
    private final Map<String, ConstructorNode> constructors = new HashMap<String, ConstructorNode>()
    private final Map<String, MethodNode> methods = new HashMap<String, MethodNode>()
    private final Map<String, Statement> statements = new HashMap<String, Statement>()
    private final Map<String, Expression> expressions = new HashMap<String, Expression>()
    final VividVisitCallback visitCallback = new VividVisitCallback()


    VividAstInspector() {
        classLoader = new MyClassLoader( VividAstInspector.class.getClassLoader(), null )
    }

    void load( File sourceFile ) throws CompilationFailedException {
        reset()

        try {
            classLoader.parseClass( sourceFile )
        } catch ( IOException e ) {
            throw new AstInspectorException( "cannot read source file", e )
        } catch ( AstSuccessfullyCaptured e ) {
            indexAstNodes()
            return
        }

        throw new AstInspectorException( "internal error" )
    }

    Expression getExpression( String name ) {
        return getNode( expressions, name )
    }

    @SuppressWarnings( "unchecked" )
    private void indexAstNodes() {
        visitor.visitBlockStatement( module.getStatementBlock() )

        for ( MethodNode method : module.getMethods() )
            visitor.visitMethod( method )

        for ( ClassNode clazz : module.getClasses() )
            visitor.visitClass( clazz )
    }

    private void reset() {
        module = null
        markedNodes.clear()
        classes.clear()
        fields.clear()
        properties.clear()
        constructors.clear()
        methods.clear()
        statements.clear()
        expressions.clear()
    }

    private <T extends ASTNode> void addNode( Map<String, T> map, String name, T node ) {
        if ( !map.containsKey( name ) ) map.put( name, node )
    }

    private <T> T getNode( Map<String, T> nodes, String key ) {
        T node = nodes.get( key )
        if ( node == null && throwOnNodeNotFound )
            throw new AstInspectorException(
                    String.format( "cannot find a node named '%s' of the requested kind", key ) )
        return node
    }

    private class MyClassLoader extends GroovyClassLoader {
        MyClassLoader( ClassLoader parent, CompilerConfiguration config ) {
            super( parent, config )
        }

        @Override
        protected CompilationUnit createCompilationUnit( CompilerConfiguration config, CodeSource source ) {
            CompilationUnit unit = super.createCompilationUnit( config, source )
            unit.addPhaseOperation( new CompilationUnit.SourceUnitOperation() {
                @Override
                void call( SourceUnit sourceUnit ) throws CompilationFailedException {
                    VividAstInspector.this.module = sourceUnit.getAST()
                    SourceLookup sourceLookup = new SourceLookup( sourceUnit )
                    VividAstInspector.this.visitCallback.codeCollector = new SpecSourceCodeCollector( sourceLookup )
                    throw new AstSuccessfullyCaptured()
                }
            }, compilePhase.getPhaseNumber() )
            return unit
        }
    }

    class MyVisitor extends ClassCodeVisitorSupport {
        @Override
        @SuppressWarnings( "unchecked" )
        void visitAnnotations( AnnotatedNode node ) {
            for ( AnnotationNode an : node.getAnnotations() ) {
                ClassNode cn = an.getClassNode()

                // this comparison should be good enough, and also works in phase conversion
                if ( cn.getNameWithoutPackage().equals( Inspect.class.getSimpleName() ) ) {
                    ConstantExpression name = ( ConstantExpression ) an.getMember( "value" )
                    if ( name == null || !( name.getValue() instanceof String ) )
                        throw new AstInspectorException( "@Inspect must have a String argument" )
                    addNode( markedNodes, ( String ) name.getValue(), node )
                    break
                }
            }

            super.visitAnnotations( node )
        }

        @Override
        void visitClass( ClassNode node ) {
            addNode( classes, node.getNameWithoutPackage(), node )
            super.visitClass( node )
        }

        @Override
        void visitField( FieldNode node ) {
            addNode( fields, node.getName(), node )
            super.visitField( node )
        }

        @Override
        void visitProperty( PropertyNode node ) {
            addNode( properties, node.getName(), node )
            super.visitProperty( node )
        }

        @Override
        protected void visitConstructorOrMethod( MethodNode node, boolean isConstructor ) {
            // ClassCodeVisitorSupport doesn't seem to visit parameters
            for ( Parameter param : node.getParameters() ) {
                visitAnnotations( param )
                if ( param.getInitialExpression() != null )
                    param.getInitialExpression().visit( this )
            }
            super.visitConstructorOrMethod( node, isConstructor )
        }

        @Override
        void visitConstructor( ConstructorNode node ) {
            addNode( constructors, node.getDeclaringClass().getNameWithoutPackage(), node )
            super.visitConstructor( node )
        }

        @Override
        void visitMethod( MethodNode node ) {
            visitCallback.onMethodEntry( node )
            addNode( methods, node.getName(), node )
            super.visitMethod( node )
            visitCallback.onMethodExit()
        }

        @Override
        void visitStatement( Statement node ) {
            if ( node.getStatementLabel() != null ) {
                addNode( statements, node.getStatementLabel(), node )
                if ( node instanceof ExpressionStatement ) {
                    Expression expression = ( ( ExpressionStatement ) node ).getExpression()
                    addExpressionNode( node.getStatementLabel(), expression )
                }
            }
            super.visitStatement( node )
        }

        @Override
        void visitMethodCallExpression( MethodCallExpression node ) {
            if ( node.isImplicitThis() ) {
                doVisitMethodCall( node )
            }
            super.visitMethodCallExpression( node )
        }

        @Override
        void visitStaticMethodCallExpression( StaticMethodCallExpression node ) {
            // note: we don't impose any constraints on the receiver type here
            doVisitMethodCall( node )
            super.visitStaticMethodCallExpression( node )
        }

        private void doVisitMethodCall( Expression node ) {
            String methodName = AstUtil.getMethodName( node )
            if ( methodName != null && methodName.startsWith( EXPRESSION_MARKER_PREFIX ) ) {
                ArgumentListExpression args = ( ArgumentListExpression ) AstUtil.getArguments( node )
                if ( args != null && args.getExpressions().size() == 1 ) {
                    String name = methodName.substring( EXPRESSION_MARKER_PREFIX.length() )
                    addExpressionNode( name, args.getExpressions().get( 0 ) )
                }
            }
        }

        private addExpressionNode( String name, Expression expression ) {
            addNode( expressions, name, expression )
            visitCallback.addExpressionNode( name, expression )
        }

        @Override
        protected SourceUnit getSourceUnit() {
            throw new AstInspectorException( "internal error" )
        }
    }

    private static class AstSuccessfullyCaptured extends Error {}
}
