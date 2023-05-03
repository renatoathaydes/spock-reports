package com.athaydes.spockframework.report.internal

import groovy.transform.CompileStatic
import org.spockframework.runtime.model.ErrorInfo
import org.spockframework.runtime.model.FeatureInfo
import org.spockframework.runtime.model.IterationInfo
import org.spockframework.runtime.model.SpecInfo

import java.util.function.Function

/**
 * Data collected for a Spock Specification.
 */
@CompileStatic
class SpecData {
    private final List<FeatureRun> featureRuns = [ ].asSynchronized() as List<FeatureRun>
    final SpecInfo info
    final long startTime
    long totalTime
    ErrorInfo initializationError
    ErrorInfo cleanupSpecError

    SpecData( SpecInfo info ) {
        this.info = info
        this.startTime = System.currentTimeMillis()
    }

    def <T> T withFeatureRuns( Function<List<FeatureRun>, T> action ) {
        synchronized ( featureRuns ) {
            return action.apply( featureRuns )
        }
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

    /**
     * Copy the failuresByIteration Map.
     * Use this method to be able to safely iterate over the Map.
     * @return a copy of the failuresByIteration.
     */
    Map<IterationInfo, List<SpecProblem>> copyFailuresByIteration() {
        synchronized ( failuresByIteration ) {
            return new LinkedHashMap<>( failuresByIteration )
        }
    }

    /**
     * Copy the timeByIteration Map.
     * Use this method to be able to safely iterate over the Map.
     * @return a copy of the timeByIteration.
     */
    Map<IterationInfo, Long> copyTimeByIteration() {
        synchronized ( timeByIteration ) {
            return new LinkedHashMap<>( timeByIteration )
        }
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
