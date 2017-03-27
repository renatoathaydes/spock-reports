package com.athaydes.spockframework.report.vivid

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.codehaus.groovy.ast.MethodNode
import org.spockframework.runtime.model.BlockKind

@ToString
@CompileStatic
class SpecSourceCode {

    private final Map<String, FeatureSourceCode> features = [ : ]

    void addLine( MethodNode feature, BlockKind blockKind, String line ) {
        features.get( feature.name, new FeatureSourceCode() )
                .blocks
                .get( blockKind, [ ] )
                .add( line )
    }

    List<String> getLines( String featureName, BlockKind blockKind ) {
        features.get( featureName, new FeatureSourceCode() )
                .blocks
                .get( blockKind, [ ] )
    }

}

class FeatureSourceCode {
    // FIXME block kind is not unique
    final Map<BlockKind, List<String>> blocks = [ : ]
}
