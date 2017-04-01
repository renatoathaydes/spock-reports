package com.athaydes.spockframework.report.vivid

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.transform.ToString
import org.codehaus.groovy.ast.MethodNode

@ToString
@CompileStatic
class SpecSourceCode {

    private final Map<String, FeatureSourceCode> features = [ : ]

    void startBlock( MethodNode feature ) {
        features.get( feature.name, new FeatureSourceCode() ).startBlock()
    }

    void addStatement( MethodNode feature, String statement ) {
        statement = removeIndent( statement )
        features[ feature.name ].addStatement( statement )
        println "Adding to ${feature.name}: $statement"
    }

    List<String> getLines( String featureName, int blockIndex ) {
        features.get( featureName )?.getBlockStatements( blockIndex ) ?: [ ]
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
    private final List<List<String>> blocks = [ ]

    void startBlock() {
        blocks << [ ]
    }

    void addStatement( String statement ) {
        blocks.last().add( statement )
        println "ALL BLOCKS: $blocks"
    }

    List<String> getBlockStatements( int blockIndex ) {
        blocks[ blockIndex ]
    }
}
