package com.athaydes.spockframework.report.vivid

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.codehaus.groovy.ast.MethodNode
import org.spockframework.util.Nullable

@ToString
@Slf4j
@CompileStatic
class SpecSourceCode {

    private final Map<String, FeatureSourceCode> features = [ : ]

    void startBlock( MethodNode feature, String label, @Nullable String text ) {
        features.get( feature.name, new FeatureSourceCode() ).startBlock( label, text )
    }

    void addStatement( MethodNode feature, String statement ) {
        def currentFeature = features[ feature.name ]
        if ( currentFeature ) {
            statement = removeIndent( statement )
            currentFeature.addStatement( statement )
        } else {
            log.debug( "Skipping statement on method {}, not a test method?", feature?.name )
        }
    }

    List<BlockCode> getBlocks( String featureName ) {
        features.get( featureName )?.blocks ?: [ ]
    }

    static String removeIndent( String code ) {
        def lines = code.readLines()
        if ( lines.size() < 2 ) {
            return code
        }

        // do not use the first line because the first line never gets any indentation
        def firstTextIndexes = lines[ 1..-1 ].collect { String line -> line.findIndexOf { it != ' ' } }
        def minIndent = firstTextIndexes.min()

        if ( minIndent > 0 ) {
            def resultLines = [ lines[ 0 ] ] + lines[ 1..-1 ].collect { String line -> line.substring( minIndent ) }
            return resultLines.join( '\n' )
        } else {
            return code
        }
    }

}

@ToString
@CompileStatic
@PackageScope
class FeatureSourceCode {
    private final List<BlockCode> blocks = [ ]

    void startBlock( String label, @Nullable String text ) {
        blocks << new BlockCode( label, text, [ ] )
    }

    void addStatement( String statement ) {
        blocks.last().statements.add( statement )
    }

    List<BlockCode> getBlocks() {
        return this.@blocks.asImmutable()
    }
}

@Canonical
class BlockCode {
    final String label
    @Nullable
    final String text
    final List<String> statements
}
