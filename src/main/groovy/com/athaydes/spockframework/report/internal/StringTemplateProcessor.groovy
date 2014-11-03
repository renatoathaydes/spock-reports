package com.athaydes.spockframework.report.internal

import groovy.transform.CompileStatic
import org.spockframework.runtime.extension.builtin.UnrollNameProvider
import org.spockframework.runtime.model.FeatureInfo
import org.spockframework.runtime.model.IterationInfo

/**
 *
 */
class StringTemplateProcessor {

    @CompileStatic
    String process( String input, List<String> dataVariables, IterationInfo iteration ) {
        def tempFeature = new FeatureInfo()
        tempFeature.name = input
        for ( variable in dataVariables ) {
            tempFeature.addParameterName( variable )
        }
        tempFeature.iterationNameProvider = new UnrollNameProvider( tempFeature, input )
        tempFeature.iterationNameProvider.getName( iteration )
    }

}
