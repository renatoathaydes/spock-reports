package com.athaydes.spockframework.report.vivid

import groovy.transform.CompileStatic
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
import org.spockframework.util.Nullable
import org.spockframework.util.inspector.AstInspector
import org.spockframework.util.inspector.AstInspectorException
import org.spockframework.util.inspector.Inspect

import java.security.CodeSource

/**
 * Based on org.spockframework.util.inspector.AstInspector by Peter Niederwieser
 */
@CompileStatic
class VividAstInspector extends AstInspector {

    static final String EXPRESSION_MARKER_PREFIX = "inspect_"

    private CompilePhase compilePhase = CompilePhase.CONVERSION
    private boolean throwOnNodeNotFound = false
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

    @Nullable
    Expression getExpression( String name ) {
        def node = getNode( expressions, name )
        if ( node instanceof Expression ) {
            return node as Expression
        }

        return null
    }

    @SuppressWarnings( "unchecked" )
    private void indexAstNodes() {
        visitor.visitBlockStatement( module.statementBlock )

        for ( MethodNode method : module.methods )
            visitor.visitMethod( method )

        for ( ClassNode clazz : module.classes )
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

    private static void addNode( Map<String, ? extends ASTNode> map, String name, ASTNode node ) {
        if ( !map.containsKey( name ) ) map.put( name, node )
    }

    @Nullable
    private getNode( Map nodes, String key ) {
        def node = nodes[ key ]
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

            // Groovy cannot see these fields from the nested class below, so let's use some Closures to help it
            final setModule = { ModuleNode mod -> module = mod }
            final setCodeCollector = { SpecSourceCodeCollector c -> visitCallback.codeCollector = c }

            unit.addPhaseOperation( new CompilationUnit.SourceUnitOperation() {
                @Override
                void call( SourceUnit sourceUnit ) throws CompilationFailedException {
                    setModule sourceUnit.AST
                    SourceLookup sourceLookup = new SourceLookup( sourceUnit )
                    setCodeCollector new SpecSourceCodeCollector( sourceLookup )
                    throw new AstSuccessfullyCaptured()
                }
            }, compilePhase.phaseNumber )
            return unit
        }
    }

    class MyVisitor extends ClassCodeVisitorSupport {
        @Override
        @SuppressWarnings( "unchecked" )
        void visitAnnotations( AnnotatedNode node ) {
            for ( AnnotationNode an in node.annotations ) {
                ClassNode cn = an.classNode

                // this comparison should be good enough, and also works in phase conversion
                if ( cn.nameWithoutPackage == Inspect.simpleName ) {
                    ConstantExpression name = ( ConstantExpression ) an.getMember( "value" )
                    if ( name == null || !( name.value instanceof String ) )
                        throw new AstInspectorException( "@Inspect must have a String argument" )
                    //noinspection UnnecessaryQualifiedReference
                    VividAstInspector.addNode( markedNodes, ( String ) name.value, node )
                    break
                }
            }

            super.visitAnnotations( node )
        }

        @Override
        void visitClass( ClassNode node ) {
            //noinspection UnnecessaryQualifiedReference
            VividAstInspector.addNode( classes, node.nameWithoutPackage, node )
            super.visitClass( node )
        }

        @Override
        void visitField( FieldNode node ) {
            //noinspection UnnecessaryQualifiedReference
            VividAstInspector.addNode( fields, node.name, node )
            super.visitField( node )
        }

        @Override
        void visitProperty( PropertyNode node ) {
            //noinspection UnnecessaryQualifiedReference
            VividAstInspector.addNode( properties, node.name, node )
            super.visitProperty( node )
        }

        @Override
        protected void visitConstructorOrMethod( MethodNode node, boolean isConstructor ) {
            // ClassCodeVisitorSupport doesn't seem to visit parameters
            for ( Parameter param : node.parameters ) {
                visitAnnotations( param )
                param.initialExpression?.visit( this )
            }
            super.visitConstructorOrMethod( node, isConstructor )
        }

        @Override
        void visitConstructor( ConstructorNode node ) {
            //noinspection UnnecessaryQualifiedReference
            VividAstInspector.addNode( constructors, node.declaringClass.nameWithoutPackage, node )
            super.visitConstructor( node )
        }

        @Override
        void visitMethod( MethodNode node ) {
            visitCallback.onMethodEntry( node )

            //noinspection UnnecessaryQualifiedReference
            VividAstInspector.addNode( methods, node.getName(), node )
            super.visitMethod( node )
            visitCallback.onMethodExit()
        }

        @Override
        void visitStatement( Statement node ) {
            if ( node.statementLabel ) {
                //noinspection UnnecessaryQualifiedReference
                VividAstInspector.addNode( statements, node.statementLabel, node )
                if ( node instanceof ExpressionStatement ) {
                    Expression expression = ( ( ExpressionStatement ) node ).expression
                    addExpressionNode( node.statementLabel, expression )
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
            if ( methodName?.startsWith( EXPRESSION_MARKER_PREFIX ) ) {
                ArgumentListExpression args = ( ArgumentListExpression ) AstUtil.getArguments( node )
                if ( args != null && args.expressions.size() == 1 ) {
                    String name = methodName.substring( EXPRESSION_MARKER_PREFIX.length() )
                    addExpressionNode( name, args.expressions.get( 0 ) )
                }
            }
        }

        private addExpressionNode( String name, Expression expression ) {
            //noinspection UnnecessaryQualifiedReference
            VividAstInspector.addNode( expressions, name, expression )
            visitCallback.addExpressionNode( name, expression )
        }

        @Override
        protected SourceUnit getSourceUnit() {
            throw new AstInspectorException( "internal error" )
        }
    }

    private static class AstSuccessfullyCaptured extends Error {}
}
