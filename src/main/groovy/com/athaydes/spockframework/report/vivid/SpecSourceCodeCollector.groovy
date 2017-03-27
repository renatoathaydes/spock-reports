package com.athaydes.spockframework.report.vivid

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.Expression
import org.spockframework.compiler.SourceLookup
import org.spockframework.runtime.model.BlockKind
import org.spockframework.util.Nullable

@CompileStatic
class SpecSourceCodeCollector {

    private static final String[] IGNORED_LABELS = [ "where" ]
    private static final Map<String, String> MAPPING = [ given: "setup", and: ( String ) null ]

    private final SourceLookup sourceLookup
    private final Map<String, SpecSourceCode> specSourceCodeByClassName = [ : ]
    private BlockKind lastBlockKind

    // the current class being parsed
    private String className

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

    void addExpression( MethodNode feature, String label, Expression expression ) {
        if ( IGNORED_LABELS.contains( label ) ) {
            return
        }

        BlockKind blockKind = toBlockKind( label )
        if ( blockKind ) {
            lastBlockKind = blockKind
        }

        String sourceLine = toSourceCode( expression )
        specSourceCodeByClassName[ className ].addLine( feature, lastBlockKind, sourceLine )
    }

    static BlockKind toBlockKind( String label ) {
        String kind = MAPPING.get( label, label )
        return kind ? BlockKind.valueOf( kind.toUpperCase() ) : null
    }

    private String toSourceCode( Expression expression ) {
        String source = sourceLookup.lookup( expression )
        return isInQuotationMarks( source ) ? source[ 1..-2 ] : source
    }

    private static boolean isInQuotationMarks( String source ) {
        return source && source.startsWith( '"' ) && source.endsWith( '"' )
    }
}
