package com.athaydes.spockframework.report.internal

import groovy.transform.CompileStatic
import org.spockframework.runtime.model.ErrorInfo
import org.spockframework.runtime.model.FeatureInfo
import org.spockframework.runtime.model.IterationInfo
import org.spockframework.runtime.model.SpecInfo

/**
 * Data collected for a Spock Specification.
 */
@CompileStatic
class SpecData {
    final SpecInfo info
    final List<FeatureRun> featureRuns = [ ].asSynchronized() as List<FeatureRun>
    long totalTime
    ErrorInfo initializationError
    ErrorInfo cleanupSpecError

    SpecData( SpecInfo info ) {
        this.info = info
    }
}

/**
 * Data related to a single feature run in a Specification.
 */
@CompileStatic
class FeatureRun {
    final FeatureInfo feature
    final Map<IterationInfo, List<SpecProblem>> failuresByIteration = [ : ].asSynchronized()
    final Map<IterationInfo, Long> timeByIteration = [ : ].asSynchronized()

    FeatureRun( FeatureInfo feature ) {
        this.feature = feature
    }

    int iterationCount() {
        failuresByIteration.size()
    }
}

/**
 * Information about an error that occurred within a feature run.
 */
@CompileStatic
class SpecProblem {

    final ErrorInfo failure

    SpecProblem( ErrorInfo failure ) {
        this.failure = failure
    }

    FailureKind getKind() {
        failure.exception instanceof AssertionError ? FailureKind.FAILURE : FailureKind.ERROR
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