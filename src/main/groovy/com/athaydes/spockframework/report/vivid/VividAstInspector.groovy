package com.athaydes.spockframework.report.vivid


import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.*
import org.spockframework.util.Nullable
import org.spockframework.util.inspector.AstInspectorException

import java.security.CodeSource
import java.util.concurrent.ConcurrentHashMap

/**
 * Based on org.spockframework.util.inspector.AstInspector by Peter Niederwieser
 */
@CompileStatic
@Slf4j
class VividAstInspector {

    private CompilePhase compilePhase = CompilePhase.CONVERSION
    private final VividClassLoader classLoader
    private final Map<File, SpecSourceCodeCollector> specCodesByFile = [ : ] as ConcurrentHashMap

    VividAstInspector() {
        classLoader = new VividClassLoader( this.class.classLoader )
    }

    @Nullable
    SpecSourceCode load( @Nullable File sourceFile, String className ) {
        // first, check if a previous spec file contained this class
        for ( codeCollector in specCodesByFile.values() ) {
            def code = codeCollector.getResultFor( className )
            if ( code ) {
                log.debug( "Found source code for $className in previously parsed file" )
                return code
            }
        }

        if ( sourceFile == null ) {
            log.warn( "Cannot find source code for spec $className" )
            log.info( "Perhaps you need to set the 'com.athaydes.spockframework.report.testSourceRoots' property? " +
                    "(the default is src/test/groovy)" )
            return null
        }

        boolean alreadyVisited = specCodesByFile.containsKey( sourceFile )

        if ( alreadyVisited ) {
            log.debug( "Cancelling visit to source file, already seen it: $sourceFile" )
            return null
        }

        log.debug "Trying to read source file $sourceFile"

        try {
            classLoader.parseClass( sourceFile )
        } catch ( IOException e ) {
            throw new AstInspectorException( "cannot read source file", e )
        } catch ( AstSuccessfullyCaptured captured ) {
            def source = getSpecSource( captured.codeCollector )
            specCodesByFile[ sourceFile ] = source
            return source.getResultFor( className )
        }

        throw new AstInspectorException( "internal error" )
    }

    private SpecSourceCodeCollector getSpecSource( SpecSourceCodeCollector codeCollector ) {
        final visitor = new VividASTVisitor( codeCollector )
        final module = codeCollector.module

        visitor.visitBlockStatement( module.statementBlock )

        for ( MethodNode method in module.methods ) {
            visitor.visitMethod( method )
        }

        for ( ClassNode clazz in module.classes ) {
            visitor.visitClass( clazz )
        }

        codeCollector
    }

    private class VividClassLoader extends GroovyClassLoader {
        VividClassLoader( ClassLoader parent ) {
            super( parent, null )
        }

        @Override
        protected CompilationUnit createCompilationUnit( CompilerConfiguration config, CodeSource source ) {
            CompilationUnit unit = super.createCompilationUnit( config, source )

            unit.addPhaseOperation( new CompilationUnit.SourceUnitOperation() {
                @Override
                void call( SourceUnit sourceUnit ) throws CompilationFailedException {
                    throw new AstSuccessfullyCaptured( new SpecSourceCodeCollector( sourceUnit ) )
                }
            }, compilePhase.phaseNumber )
            return unit
        }
    }

    private static class AstSuccessfullyCaptured extends Error {
        final SpecSourceCodeCollector codeCollector

        AstSuccessfullyCaptured( SpecSourceCodeCollector codeCollector ) {
            super()
            this.codeCollector = codeCollector
        }

    }

}

@CompileStatic
class VividASTVisitor extends ClassCodeVisitorSupport {

    private final SpecSourceCodeCollector codeCollector
    private boolean visitStatements = false

    @Nullable
    private String currentLabel = null

    VividASTVisitor( SpecSourceCodeCollector codeCollector ) {
        this.codeCollector = codeCollector
    }

    @Override
    void visitClass( ClassNode node ) {
        codeCollector.className = node.name
        super.visitClass( node )
    }

    @Override
    void visitMethod( MethodNode node ) {
        def previousIsTestMethod = visitStatements
        visitStatements = node.isPublic() && !node.isStatic()

        if ( visitStatements ) {
            currentLabel = null
            codeCollector.method = node
        }

        super.visitMethod( node )

        codeCollector.method = null

        visitStatements = previousIsTestMethod
    }

    @Override // This is overridden to avoid visiting annotations.
    void visitAnnotations(AnnotatedNode node) {
        // do nothing - we don't want to visit annotations (see #231)
    }

    @Override
    void visitStatement( Statement node ) {
        if ( visitStatements && node instanceof BlockStatement ) {
            def statements = filterStatements( ( node as BlockStatement ).statements )
            /*
                First we find the smallest common indent levels for all
                code lines within a code block (excluding block statements),
                which will be trimmed from the block lines when added to the code collector.
             */
            int blockIndent = Integer.MAX_VALUE // start with the maximum possible indent.
            List<Statement> blockStatements = [] // the statements of a block (given, when, then, etc.)

            for ( statement in statements ) {
                boolean updateIndent = true // Only update indent if the statement is not the block statement itself
                if ( statement.statementLabel ) {
                    def labelText = SpecSourceCodeCollector.stringConstant( statement )
                    if ( labelText ) {
                        for ( blockStatement in blockStatements )
                            codeCollector.add( blockStatement, blockIndent - 1 )

                        blockStatements.clear() // The block code was collected, we clear it for the next block.
                        blockIndent = Integer.MAX_VALUE // The block statements are over, reset indent.
                        updateIndent = false // we also don't want the block (label) to be determining the common indent level
                    }
                }
                blockStatements << statement // we add the statement to the block statements

                if ( updateIndent )
                    blockIndent = Math.min( blockIndent, statement.columnNumber )
            }

            // We collect the last block of statements:
            for ( blockStatement in blockStatements )
                codeCollector.add( blockStatement, blockIndent - 1 )

            visitStatements = false
        }
        super.visitStatement( node )
    }

    private static List<Statement> filterStatements( List<Statement> statements ) {
        def filtered = []
        def waitForNextBlock = false
        if ( statements )
            for (statement in statements) {
                if (waitForNextBlock && !statement.statementLabel)
                    continue // skip statements in this block
                else
                    waitForNextBlock = false

                filtered << statement

                if (statement.statementLabel == 'where')
                    waitForNextBlock = true
            }

        return filtered
    }

    @Override
    protected SourceUnit getSourceUnit() {
        throw new AstInspectorException( "internal error" )
    }
}

