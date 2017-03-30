package com.athaydes.spockframework.report.vivid

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassCodeVisitorSupport
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.ModuleNode
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilationFailedException
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.SourceUnit
import org.spockframework.compiler.SourceLookup
import org.spockframework.util.Nullable
import org.spockframework.util.inspector.AstInspectorException
import org.spockframework.util.inspector.Inspect

import java.lang.reflect.Method
import java.security.CodeSource

import static ExpressionParser.parseExpression

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

        private int blockIndex = 0
        private boolean isTestMethod = false

        @Nullable
        private String currentLabel = null

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
            def previousIsTestMethod = isTestMethod
            isTestMethod = node.isPublic() && node.parameters.size() == 0
            println "Visiting method ${node.name}, is test? $isTestMethod, previous: $previousIsTestMethod"

            if ( isTestMethod ) {
                blockIndex = 0
                currentLabel = null
                visitCallback.onMethodEntry( node )
            }

            super.visitMethod( node )

            if ( isTestMethod ) {
                visitCallback.onMethodExit()
            }

            println "done visiting method ${node.name}, setting isTestMethod to ${previousIsTestMethod}"
            isTestMethod = previousIsTestMethod
        }

        @Override
        void visitStatement( Statement node ) {
            if ( !isTestMethod || ( blockIndex == 0 && node instanceof BlockStatement ) ) {
                // not in test method or expected first statement in a block, just keep going
            } else {
                println "Checking statement as it is test method? $isTestMethod"
                Expression expression = node instanceof Expression ?
                        node as Expression :
                        parseExpression( node )

                if ( expression != null ) {
                    if ( node.statementLabel != currentLabel ) {
                        // do not set label to null if the node has no label

                        currentLabel = node.statementLabel ?: currentLabel

                    } else {
                        blockIndex++
                    }

                    // only add expression if it's not the first one or it's not a constant,
                    // otherwise we just capture the block.text
                    if ( blockIndex > 0 || !isStringConstant( expression ) ) {
                        addExpressionNode( currentLabel, expression )
                    }
                }
            }

            super.visitStatement( node )
        }

        private static boolean isStringConstant( Expression expression ) {
            expression instanceof ConstantExpression && expression.type.name == 'java.lang.String'
        }

        private addExpressionNode( String name, Expression expression ) {
            visitCallback.addExpressionNode( blockIndex, name, expression )
        }

        @Override
        protected SourceUnit getSourceUnit() {
            throw new AstInspectorException( "internal error" )
        }
    }

    private static class AstSuccessfullyCaptured extends Error {}
}

// FIXME for each subtype of Statement, this class should try to extract an Expression
// which can be converted to the original source code
@CompileStatic
class ExpressionParser {

    // try these methods dynamically for now!
    final methodNames = [ 'getExpression', 'getBooleanExpression' ] as Set

    static Expression parseExpression( Statement statement ) {

        def candidateMethods = statement.class.methods.findAll { Method m ->
            m.parameterCount == 0 && m.name in methodNames
        }

        for ( method in candidateMethods ) {
            if ( statement ) {
                try {
                    return method.invoke( statement ) as Expression
                } catch ( e ) {
                    e.printStackTrace()
                }
            }
        }

        // no luck
        null
    }
}
