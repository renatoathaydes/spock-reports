package com.athaydes.spockframework.report.util

import com.athaydes.spockframework.report.internal.FeatureRun
import org.spockframework.runtime.model.BlockKind
import org.spockframework.runtime.model.FeatureInfo
import org.spockframework.runtime.model.IterationInfo

final class Formatter {

    private static final Map blocksMapping = [
            ( BlockKind.SETUP )  : 'Given:',
            ( BlockKind.CLEANUP ): 'Cleanup:',
            ( BlockKind.THEN )   : 'Then:',
            ( BlockKind.EXPECT ) : 'Expect:',
            ( BlockKind.WHEN )   : 'When:',
            ( BlockKind.WHERE )  : 'Where:',
            'AND'                : 'And:',
            'EXAMPLES'           : 'Examples:'
    ]

    static blockToString(def blockName){
        blocksMapping[blockName]
    }

    static String featureNameFrom(FeatureInfo feature, IterationInfo iteration, int index ) {
        if ( feature.iterationNameProvider && iteration.dataValues?.length > 0 ) {
            def name = feature.iterationNameProvider.getName( iteration )
            // reset the index instance to fix #70
            def nameMatcher = name =~ /(.*)\[\d+\]$/
            if ( nameMatcher.matches() ) {
                def rawName = nameMatcher.group( 1 )
                return "$rawName [$index]"
            } else {
                return name
            }
        } else {
            return feature.name
        }
    }

    static String iterationsResult(FeatureRun run ) {
        def totalErrors = run.failuresByIteration.values().count { List it -> !it.empty }
        "${run.iterationCount() - totalErrors}/${run.iterationCount()} passed"
    }
}
