package com.athaydes.spockframework.report.internal;

@Singleton
class EmptyInitializationException extends Exception {

    static final String INIT_ERROR = "INITIALIZATION ERROR"

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this
    }

    @Override
    public String toString() {
        INIT_ERROR
    }
}
