package com.athaydes.spockframework.report.util

import com.athaydes.spockframework.report.internal.FailureKind
import com.athaydes.spockframework.report.internal.FeatureRun
import com.athaydes.spockframework.report.internal.SpecData
import com.athaydes.spockframework.report.internal.SpecProblem
import org.spockframework.runtime.model.FeatureInfo
import org.spockframework.runtime.model.IterationInfo
import spock.lang.Unroll

import java.lang.annotation.Annotation

class Utils {

    static double successRate( int total, int reproved ) {
        double dTotal = total
        double dReproved = reproved
        Math.min( 1.0D, Math.max( 0.0D, ( dTotal > 0D ? ( dTotal - dReproved ) / total : 1.0D ) ) )
    }

    static Map stats( SpecData data ) {
        def failures = countProblems( data.featureRuns, this.&isFailure )
        def errors = countProblems( data.featureRuns, this.&isError )
        def skipped = data.info.allFeaturesInExecutionOrder.count { FeatureInfo f -> f.skipped }
        def total = countFeatures( data.featureRuns )
        def successRate = successRate( total, ( errors + failures ).toInteger() )
        [ failures   : failures, errors: errors, skipped: skipped, totalRuns: total,
          successRate: successRate, time: data.totalTime ]
    }

    static Map aggregateStats( Map<String, Map> aggregatedData ) {
        def result = [ total: 0, passed: 0, failed: 0, fFails: 0, fErrors: 0, time: 0.0 ]
        aggregatedData.values().each { Map json ->
            def stats = json.stats
            def isFailure = stats.failures + stats.errors > 0
            result.total += 1
            result.passed += ( isFailure ? 0 : 1 )
            result.failed += ( isFailure ? 1 : 0 )
            result.fFails += stats.failures
            result.fErrors += stats.errors
            result.time += stats.time
        }
        result.successRate = successRate( result.total, result.failed )
        result
    }

    static boolean isUnrolled( FeatureInfo feature ) {
        feature.spec?.isAnnotationPresent( Unroll ) ||
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

    static List<Map> problemsByIteration( Map<IterationInfo, List<SpecProblem>> failures ) {
        failures.inject( [ ] ) { List<Map> acc, iteration, List<SpecProblem> failureList ->
            def allErrors = failureList.collect { SpecProblem it -> it.failure.exception }
            acc << [ dataValues: iteration.dataValues, errors: allErrors ]
        }
    }

    static <A extends Annotation> A specAnnotation( SpecData data, Class<A> annotation ) {
        data.info.description?.testClass?.getAnnotation( annotation )
    }

    static Map createAggregatedData( List<FeatureInfo> executedFeatures,
                                     List<FeatureInfo> ignoredFeatures,
                                     Map stats ) {
        [
                executedFeatures: executedFeatures?.name?.sort() ?: [ ],
                ignoredFeatures : ignoredFeatures?.name?.sort() ?: [ ],
                stats           : stats
        ]
    }

    @Deprecated
    static String getSpecClassName( SpecData data ) {
        Files.getSpecClassName( data )
    }
}
