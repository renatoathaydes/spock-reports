package com.athaydes.spockframework.report.vivid

import com.athaydes.spockframework.report.vivid.VividAstInspector.AstSuccessfullyCaptured
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.codehaus.groovy.ast.ClassCodeVisitorSupport
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilationFailedException
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.SourceUnit
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

    @Override
    void visitStatement( Statement node ) {
        if ( visitStatements && node instanceof BlockStatement ) {
            def stmts = ( node as BlockStatement ).statements
            def waitForNextBlock = false
            if ( stmts ) for ( statement in stmts ) {
                if ( waitForNextBlock && !statement.statementLabel ) {
                    continue // skip statements in this block
                } else {
                    waitForNextBlock = false
                }

                codeCollector.add( statement )

                if ( statement.statementLabel == 'where' ) {
                    waitForNextBlock = true
                }
            }
            visitStatements = false
        }

        super.visitStatement( node )
    }

    @Override
    protected SourceUnit getSourceUnit() {
        throw new AstInspectorException( "internal error" )
    }
}

