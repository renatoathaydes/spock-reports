package com.athaydes.spockframework.report.util

import com.athaydes.spockframework.report.internal.FailureKind
import com.athaydes.spockframework.report.internal.HtmlReportCreator
import com.athaydes.spockframework.report.internal.SpecData
import com.athaydes.spockframework.report.internal.SpecProblem
import groovy.transform.CompileStatic
import org.spockframework.runtime.model.FeatureInfo
import spock.lang.Unroll

import java.lang.annotation.Annotation

@CompileStatic
class Utils {

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
        def failures = HtmlReportCreator.countProblems( data.featureRuns, this.&isFailure )
        def errors = HtmlReportCreator.countProblems( data.featureRuns, this.&isError )
        def skipped = data.info.allFeatures.count { FeatureInfo f -> f.skipped }
        def total = HtmlReportCreator.countFeatures( data.featureRuns )
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
}
