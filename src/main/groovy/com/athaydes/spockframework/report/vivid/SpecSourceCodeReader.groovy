package com.athaydes.spockframework.report.vivid

import com.athaydes.spockframework.report.internal.SpecData
import com.athaydes.spockframework.report.util.Utils
import groovy.util.logging.Slf4j
import org.spockframework.runtime.model.FeatureInfo
import org.spockframework.util.Nullable

@Slf4j
class SpecSourceCodeReader {

    String testSourceRoots = 'src/test/groovy'

    @Nullable
    private SpecSourceCode specSourceCode

    private final VividAstInspector inspector = new VividAstInspector()

    void read( SpecData data ) {
        try {
            File file = Utils.getSpecFile( testSourceRoots, data )
            specSourceCode = inspector.load( file, Utils.getSpecClassName( data ) )
        } catch ( Exception e ) {
            log.error( "Cannot create SpecSourceCode: ${e.message ?: e}", e )
        }
    }

    List<BlockCode> getBlocks( FeatureInfo feature ) {
        return specSourceCode?.getBlocks( feature.name ) ?: [ ]
    }
}
