package com.athaydes.spockframework.report.vivid

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.codehaus.groovy.ast.MethodNode

@ToString
@CompileStatic
class SpecSourceCode {

    private final Map<String, FeatureSourceCode> features = [ : ]

    void addLine( MethodNode feature, int blockIndex, String line ) {
        line = removeIndent( line )
        println "Adding ${feature.name} index = $blockIndex: $line"
        features.get( feature.name, new FeatureSourceCode() )
                .getBlockLines( blockIndex )
                .add( line )
    }

    List<String> getLines( String featureName, int blockIndex ) {
        features.get( featureName, new FeatureSourceCode() )
                .getBlockLines( blockIndex )
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
class FeatureSourceCode {
    private final List<List<String>> blocks = [ ]

    List<String> getBlockLines( int blockIndex ) {
        if ( blocks.size() < blockIndex + 1 ) {
            blocks << [ ]
        }
        assert blockIndex < blocks.size()
        blocks[ blockIndex ]
    }
}
