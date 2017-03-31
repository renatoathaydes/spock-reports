package com.athaydes.spockframework.report.vivid

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.codehaus.groovy.ast.ClassCodeVisitorSupport
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.ModuleNode
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

import java.security.CodeSource

/**
 * Based on org.spockframework.util.inspector.AstInspector by Peter Niederwieser
 */
@CompileStatic
@Slf4j
class VividAstInspector {

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
        MethodNode methodNode

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
                methodNode = node
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
            if ( isTestMethod && node instanceof BlockStatement ) {
                def stmts = ( node as BlockStatement ).statements
                if ( stmts ) for ( st in stmts ) {
                    visitCallback.codeCollector.add( methodNode, st )
                }
            }

            super.visitStatement( node )
        }

        @Override
        protected SourceUnit getSourceUnit() {
            throw new AstInspectorException( "internal error" )
        }
    }

    private static class AstSuccessfullyCaptured extends Error {}
}
