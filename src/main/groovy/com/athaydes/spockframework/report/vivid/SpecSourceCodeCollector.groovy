package com.athaydes.spockframework.report.vivid

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.Expression
import org.spockframework.compiler.SourceLookup
import org.spockframework.runtime.model.BlockKind
import org.spockframework.util.Nullable

@Slf4j
@CompileStatic
class SpecSourceCodeCollector {

    private static final Set<String> IGNORED_LABELS = ( [ "where" ] as Set ).asImmutable()

    private final SourceLookup sourceLookup
    private final Map<String, SpecSourceCode> specSourceCodeByClassName = [ : ]

    @Nullable
    // the current class being parsed
    private String className

    @Nullable
    private BlockKind previousBlockKind = null

    SpecSourceCodeCollector( SourceLookup sourceLookup ) {
        this.sourceLookup = sourceLookup
    }

    void setClassName( String className ) {
        this.@className = className
        specSourceCodeByClassName[ className ] = new SpecSourceCode()
    }

    @Nullable
    SpecSourceCode getResultFor( String className ) {
        specSourceCodeByClassName.remove( className )
    }

    void addExpression( MethodNode feature, int blockIndex, String label, Expression expression ) {
        if ( label in IGNORED_LABELS ) {
            return
        }

        BlockKind blockKind = toBlockKind( label )

        if ( !blockKind ) {
            if ( previousBlockKind ) {
                blockKind = previousBlockKind
            } else {
                log.info( "Can't find block kind for label '$label'. Will use SETUP instead" )
                blockKind = BlockKind.SETUP
            }
        }

        String sourceLine = toSourceCode( expression )

        specSourceCodeByClassName[ className ].addLine( feature, blockIndex, sourceLine )

        previousBlockKind = blockKind
    }

    @Nullable
    static BlockKind toBlockKind( String label ) {
        if ( label == null ) {
            return null
        }

        def kindName = label.toUpperCase()
        BlockKind kind = BlockKind.values().find { BlockKind it -> it.name() == kindName }

        if ( !kind && kindName == 'GIVEN' ) {
            kind = BlockKind.SETUP // GIVEN is missing for some reason
        }

        return kind
    }

    private String toSourceCode( Expression expression ) {
        sourceLookup.lookup( expression )
    }

}
