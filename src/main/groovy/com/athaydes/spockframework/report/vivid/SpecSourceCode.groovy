package com.athaydes.spockframework.report.vivid

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.transform.ToString
import org.codehaus.groovy.ast.MethodNode
import org.spockframework.runtime.model.BlockKind

@ToString
@CompileStatic
class SpecSourceCode {

    private final Map<String, FeatureSourceCode> features = [ : ]

    void addLine( MethodNode feature, BlockKey blockKey, String line ) {
        features.get( feature.name, new FeatureSourceCode() )
                .blocks
                .get( blockKey, [ ] )
                .add( line )
    }

    List<String> getLines( String featureName, BlockKey blockKey ) {
        features.get( featureName, new FeatureSourceCode() )
                .blocks
                .get( blockKey, [ ] )
    }

}

class FeatureSourceCode {
    final Map<BlockKey, List<String>> blocks = [ : ]
}

@Immutable
class BlockKey {
    BlockKind kind
    int index
}
