package com.athaydes.spockframework.report.internal

import org.junit.ComparisonFailure
import org.spockframework.runtime.model.ErrorInfo
import org.spockframework.runtime.model.FeatureInfo
import org.spockframework.runtime.model.IterationInfo
import org.spockframework.runtime.model.SpecInfo

/**
 * Data collected for a Spock Specification.
 */
class SpecData {
    SpecInfo info
    List<FeatureRun> featureRuns = [ ]
    long totalTime
    ErrorInfo initializationError
}

/**
 * Data related to a single feature run in a Specification.
 */
class FeatureRun {
    FeatureInfo feature
    Map<IterationInfo, List<SpecProblem>> failuresByIteration = [ : ]
    Map<IterationInfo, Long> timeByIteration = [ : ]

    int iterationCount() {
        failuresByIteration.size()
    }
}

/**
 * Information about an error that occurred within a feature run.
 */
class SpecProblem {

    final ErrorInfo failure

    SpecProblem( ErrorInfo failure ) {
        this.failure = failure
    }

    FailureKind getKind() {
        failure.exception instanceof AssertionError || failure.exception instanceof ComparisonFailure ?
                FailureKind.FAILURE :
                FailureKind.ERROR
    }

}

/**
 * Kind of failure for a feature run.
 *
 * An ERROR means an unexpected {@link Throwable} was thrown, while FAILURE means a test assertion failure.
 */
enum FailureKind {
    FAILURE, ERROR
}