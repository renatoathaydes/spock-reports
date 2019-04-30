package com.athaydes.spockframework.report

import spock.lang.Issue
import spock.lang.Narrative
import spock.lang.See
import spock.lang.Specification
import spock.lang.Unroll

@Narrative( """
As a developer
I want to see my code""" )
class VividFakeTest extends Specification {

    def "A first test with Then code block"() {
        given:
        "we have x and y"

        and:
        "some more things"

        when:
        "I do crazy things"

        then:
        verifyAll {
            x == y
            y == x
        }

        where:
        "The examples below are used"
        x   | y
        'a' | 'a'
        'b' | 'c'
    }

    def "Another feature without code"() {
        setup:
        "Setup block here"
        expect:
        "Expecting something ??"
    }

    def "Another feature with method call"() {
        expect:
        add( 1, 2 ) == 3
    }

    private static int add( int a, int b ) {
        return a + b
    }

    @Issue( [ "http://myhost.com/issues/995", "https://myhost.com/issues/973" ] )
    def "A test with an error"() {
        when:
        "An Exception is thrown"
        throw new RuntimeException( 'As expected' )

        then:
        "Will never succeed"
    }

    @See( [ "http://myhost.com/features/feature-234" ] )
    def "A test with a failure"() {
        when:
        "Do nothing"
        then:
        "Test fails"
        assert 3 == 2
    }

    def "A Spec without block Strings"() {
        given:
        int a = 0

        and:
        int b = 1
        int c = 2
        int d = b + c

        when:
        int e = a + b + c + d

        then:
        e == 6
        a == 0
        c == 2 * b

        and:
        c > 0
    }

    @Unroll
    def "An @Unrolled spec with x=#x and y=#y"() {
        setup:
        "nothing"
        expect:
        x == 0
        and:
        "An error if y is 5"
        if ( y == 5 ) {
            throw new RuntimeException( 'y is 5' )
        }

        where:
        x | y
        0 | 1
        2 | 3
        0 | 5
    }

}
