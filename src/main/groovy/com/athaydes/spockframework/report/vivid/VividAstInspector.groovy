package com.athaydes.spockframework.report.vivid

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassCodeVisitorSupport
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.ModuleNode
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
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
import org.spockframework.util.inspector.AstInspectorException
import org.spockframework.util.inspector.Inspect

import java.security.CodeSource

/**
 * Based on org.spockframework.util.inspector.AstInspector by Peter Niederwieser
 */
@CompileStatic
@Slf4j
class VividAstInspector {

    static final String EXPRESSION_MARKER_PREFIX = "inspect_"

    private CompilePhase compilePhase = CompilePhase.CONVERSION
    private final VividClassLoader classLoader
    private ModuleNode module
    private final VividASTVisitor visitor = new VividASTVisitor()
    private final VividVisitCallback visitCallback = new VividVisitCallback()
    private final Set<File> visitedFiles = [ ]

    VividAstInspector() {
        classLoader = new VividClassLoader( VividAstInspector.class.getClassLoader(), null )
    }

    @Nullable
    SpecSourceCode load( @Nullable File sourceFile, String className ) {
        log.debug "Trying to read source file $sourceFile"

        // spec is in same file as some other specs, but we probably already parsed the file before
        def code = visitCallback.codeCollector?.getResultFor( className )

        if ( code != null ) {
            log.debug( "Found class file in specs that had already been parsed" )
            return code
        }

        if ( sourceFile == null ) {
            log.warn( "Cannot find source code for spec $className" )
            return null
        }

        boolean alreadyVisited = !visitedFiles.add( sourceFile )

        if ( alreadyVisited ) {
            log.debug( "Cancelling visit to source file, already seen it: $sourceFile" )
            return null
        }

        try {
            classLoader.parseClass( sourceFile )
        } catch ( IOException e ) {
            throw new AstInspectorException( "cannot read source file", e )
        } catch ( AstSuccessfullyCaptured ignore ) {
            indexAstNodes()
            return visitCallback.codeCollector?.getResultFor( className )
        }

        throw new AstInspectorException( "internal error" )
    }

    private void indexAstNodes() {
        visitor.visitBlockStatement( module.statementBlock )

        for ( MethodNode method : module.methods ) {
            visitor.visitMethod( method )
        }

        for ( ClassNode clazz : module.classes ) {
            visitor.visitClass( clazz )
        }
    }

    private class VividClassLoader extends GroovyClassLoader {
        VividClassLoader( ClassLoader parent, CompilerConfiguration config ) {
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
                    setCodeCollector new SpecSourceCodeCollector( new SourceLookup( sourceUnit ) )
                    throw new AstSuccessfullyCaptured()
                }
            }, compilePhase.phaseNumber )
            return unit
        }
    }

    class VividASTVisitor extends ClassCodeVisitorSupport {

        int methodExpressionIndex = 0

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
                    break
                }
            }

            super.visitAnnotations( node )
        }

        @Override
        void visitClass( ClassNode node ) {
            visitCallback.startClass node.name
            super.visitClass( node )
        }

        @Override
        void visitMethod( MethodNode node ) {
            methodExpressionIndex = 0
            visitCallback.onMethodEntry( node )
            super.visitMethod( node )
            visitCallback.onMethodExit()
        }

        @Override
        void visitStatement( Statement node ) {
            if ( methodExpressionIndex == 0 && node instanceof BlockStatement ) {
                // expected first statement in a block, just keep going
            } else {
                if ( node instanceof ExpressionStatement ) {
                    Expression expression = ( ( ExpressionStatement ) node ).expression

                    // only add expression if it's not the first one or it's not a constant,
                    // otherwise we just capture the block.text
                    if ( methodExpressionIndex > 0 || !isStringConstant( expression ) ) {
                        addExpressionNode( node.statementLabel, expression )
                    }
                }
                methodExpressionIndex++
            }

            super.visitStatement( node )
        }

        @Override
        void visitStaticMethodCallExpression( StaticMethodCallExpression node ) {
            // note: we don't impose any constraints on the receiver type here
            doVisitMethodCall( node )
            super.visitStaticMethodCallExpression( node )
        }

        private static boolean isStringConstant( Expression expression ) {
            expression instanceof ConstantExpression && expression.type.name == 'java.lang.String'
        }

        private void doVisitMethodCall( Expression node ) {
            String methodName = AstUtil.getMethodName( node )
            if ( methodName?.startsWith( EXPRESSION_MARKER_PREFIX ) ) {
                ArgumentListExpression args = ( ArgumentListExpression ) AstUtil.getArguments( node )
                if ( args != null && args.expressions.size() == 1 ) {
                    String name = methodName.substring( EXPRESSION_MARKER_PREFIX.length() )
                    addExpressionNode( name, args.expressions.first() )
                }
            }
        }

        private addExpressionNode( String name, Expression expression ) {
            visitCallback.addExpressionNode( name, expression )
        }

        @Override
        protected SourceUnit getSourceUnit() {
            throw new AstInspectorException( "internal error" )
        }
    }

    private static class AstSuccessfullyCaptured extends Error {}
}
