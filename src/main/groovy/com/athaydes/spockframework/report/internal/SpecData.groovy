package com.athaydes.spockframework.report.internal

import org.junit.ComparisonFailure
import org.spockframework.runtime.model.ErrorInfo
import org.spockframework.runtime.model.FeatureInfo
import org.spockframework.runtime.model.IterationInfo
import org.spockframework.runtime.model.SpecInfo

/**
 *
 * User: Renato
 */
class SpecData {
    SpecInfo info
    List<FeatureRun> featureRuns = [ ]
    long totalTime
    ErrorInfo initializationError
}

class FeatureRun {
    FeatureInfo feature
    Map<IterationInfo, List<SpecProblem>> failuresByIteration = [ : ]

    int iterationCount() {
        failuresByIteration.size()
    }
}

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

enum FailureKind {
    FAILURE, ERROR
}