package com.athaydes.spockframework.report.vivid

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.codehaus.groovy.ast.MethodNode

@ToString
@CompileStatic
class SpecSourceCode {

    private final Map<String, FeatureSourceCode> features = [ : ]

    void addLine( MethodNode feature, int blockIndex, String line ) {
        println "Adding ${feature.name} index = $blockIndex: $line"
        features.get( feature.name, new FeatureSourceCode() )
                .getBlockLines( blockIndex )
                .add( line )
    }

    List<String> getLines( String featureName, int blockIndex ) {
        features.get( featureName, new FeatureSourceCode() )
                .getBlockLines( blockIndex )
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
