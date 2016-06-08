package com.athaydes.spockframework.report.internal;

@Singleton
class EmptyInitializationException extends Exception {
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this
    }

    @Override
    public String toString() {
        "INITIALIZATION ERROR"
    }
}
