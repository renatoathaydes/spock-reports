package com.athaydes.spockframework.report.internal

import spock.lang.Specification
import spock.lang.Subject

class ProblemBlockWriterSpec extends Specification {

    @Subject
    ProblemBlockWriter problemBlockWriter = new ProblemBlockWriter()

    def "Can print any kind of error in report when not printing stacktrace"() {
        given: 'A ProblemBlockWriter that does not print stacktraces'
        problemBlockWriter.printThrowableStackTrace = false

        when: 'Some error is formatted'
        def result = problemBlockWriter.formatProblemMessage error

        then: 'The result is as expected'
        result == expectedString

        where:
        error                                | expectedString
        ''                                   | ''
        'an error'                           | 'an error'
        10                                   | '10'
        null                                 | 'null'
        throwAndGet( new Exception( 'hi' ) ) | new Exception( 'hi' ).toString()
    }

    def "Can print any kind of error in report when printing stacktrace"() {
        given: 'A ProblemBlockWriter that prints stacktraces'
        problemBlockWriter.printThrowableStackTrace = true

        when: 'Some error that is NOT a Throwable is formatted'
        def result = problemBlockWriter.formatProblemMessage error

        then: 'The result is as expected'
        result == expectedString

        where:
        error      | expectedString
        ''         | ''
        'an error' | 'an error'
        10         | '10'
        null       | 'null'
    }


    def "Can print stacktrace in report"() {
        given: 'An Throwable with a real stacktrace'
        def throwable = throwAndGet new RuntimeException( "An error" )

        and: 'A ProblemBlockWriter that prints stacktraces'
        problemBlockWriter.printThrowableStackTrace = true

        when: 'The Throwable is printed using ProblemBlockWriter'
        def result = problemBlockWriter.formatProblemMessage throwable

        then: 'The given String contains the stack-trace of the Throwable'
        result.startsWith 'java.lang.RuntimeException: An error'
        result.readLines().size() > 5
        result.readLines().drop( 1 )*.trim().every { it.startsWith( 'at ' ) }
        result.contains( "at ${this.class.name}" )
    }

    static Throwable throwAndGet( Throwable throwable ) {
        try {
            throw throwable
        } catch ( Throwable t ) {
            return t
        }
    }

}
