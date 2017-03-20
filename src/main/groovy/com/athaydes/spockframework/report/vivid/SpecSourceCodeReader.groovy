package com.athaydes.spockframework.report.vivid

import com.athaydes.spockframework.report.internal.SpecData
import com.athaydes.spockframework.report.util.Utils
import groovy.util.logging.Slf4j
import org.spockframework.runtime.model.BlockInfo
import org.spockframework.runtime.model.FeatureInfo

@Slf4j
class SpecSourceCodeReader {

    String testSourceRoots = 'src/test/groovy'

    private SpecSourceCode specSourceCode = new SpecSourceCode()

    void read( SpecData data ) {
        try {
            VividAstInspector inspector = new VividAstInspector()

            File file = Utils.getSpecFile( testSourceRoots, data )
            if ( file ) {
                specSourceCode = inspector.load( file )
            } else {
                log.warn( "Could not locate the source code for Spec: ${Utils.specNameFromFileName( data.info )}" )
            }
        } catch ( Exception e ) {
            log.error( "Cannot create SpecSourceCode: $e.message", e )
        }
    }

    List<String> getLines( FeatureInfo feature, BlockInfo block ) {
        return specSourceCode.getLines( feature.name, block.kind )
    }
}
