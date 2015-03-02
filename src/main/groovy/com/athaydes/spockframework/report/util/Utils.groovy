package com.athaydes.spockframework.report.util

import com.athaydes.spockframework.report.internal.FailureKind
import com.athaydes.spockframework.report.internal.FeatureRun
import com.athaydes.spockframework.report.internal.SpecData
import com.athaydes.spockframework.report.internal.SpecProblem
import org.spockframework.runtime.model.BlockKind
import org.spockframework.runtime.model.FeatureInfo
import org.spockframework.runtime.model.IterationInfo
import spock.lang.Unroll

import java.lang.annotation.Annotation

class Utils {

    public static final Map block2String = [
            ( BlockKind.SETUP )  : 'Given:',
            ( BlockKind.CLEANUP ): 'Cleanup:',
            ( BlockKind.THEN )   : 'Then:',
            ( BlockKind.EXPECT ) : 'Expect:',
            ( BlockKind.WHEN )   : 'When:',
            ( BlockKind.WHERE )  : 'Where:',
            'AND'                : 'And:',
            'EXAMPLES'           : 'Examples:'
    ]

    static File createDir( String outputDir ) {
        def reportsDir = new File( outputDir )
        reportsDir.mkdirs()
        reportsDir
    }

    static double successRate( int total, int reproved ) {
        double dTotal = total
        double dReproved = reproved
        Math.min( 1.0D, Math.max( 0.0D, ( dTotal > 0D ? ( dTotal - dReproved ) / total : 1.0D ) ) )
    }

    static Map stats( SpecData data ) {
        def failures = countProblems( data.featureRuns, this.&isFailure )
        def errors = countProblems( data.featureRuns, this.&isError )
        def skipped = data.info.allFeatures.count { FeatureInfo f -> f.skipped }
        def total = countFeatures( data.featureRuns )
        def successRate = successRate( total, ( errors + failures ).toInteger() )
        [ failures   : failures, errors: errors, skipped: skipped, totalRuns: total,
          successRate: successRate, time: data.totalTime ]
    }

    static boolean isEmptyOrContainsOnlyEmptyStrings( List<String> strings ) {
        !strings || strings.every { String it -> it.trim() == '' }
    }

    static boolean isUnrolled( FeatureInfo feature ) {
        feature.description?.annotations?.any { Annotation a -> a.annotationType() == Unroll } ?: false
    }

    static boolean isFailure( SpecProblem problem ) {
        problem.kind == FailureKind.FAILURE
    }

    static boolean isError( SpecProblem problem ) {
        problem.kind == FailureKind.ERROR
    }

    static int countFeatures( List<FeatureRun> runs, Closure featureFilter = { true } ) {
        runs.findAll( featureFilter ).inject( 0 ) { int count, FeatureRun fr ->
            count + ( isUnrolled( fr.feature ) ? fr.iterationCount() : 1 )
        } as int
    }

    static int countProblems( List<FeatureRun> runs, Closure problemFilter ) {
        runs.inject( 0 ) { int count, FeatureRun fr ->
            def allProblems = fr.failuresByIteration.values().flatten()
            count + ( isUnrolled( fr.feature ) ?
                    allProblems.count( problemFilter ) :
                    allProblems.any( problemFilter ) ? 1 : 0 )
        } as int
    }

    static String iterationsResult( FeatureRun run ) {
        def totalErrors = run.failuresByIteration.values().count { List it -> !it.empty }
        "${run.iterationCount() - totalErrors}/${run.iterationCount()} passed"
    }

    static List<Map> problemsByIteration( Map<IterationInfo, List<SpecProblem>> failures ) {
        failures.inject( [ ] ) { List<Map> acc, iteration, List<SpecProblem> failureList ->
            def allErrors = failureList.collect { SpecProblem it -> it.failure.exception }
            if ( allErrors ) {
                acc << [ dataValues: iteration.dataValues, errors: allErrors ]
            }
            acc
        }
    }

}
