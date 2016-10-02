package com.athaydes.spockframework.report.internal

/**
 * Simply wraps a Throwable.
 *
 * Useful because AssertionErrors would be registered as failures rather than errors otherwise.
 */
class SpecInitializationError extends Exception {

    SpecInitializationError( Throwable wrapped ) {
        super( wrapped )
    }

    @Override
    public String toString() {
        cause.toString()
    }
}
