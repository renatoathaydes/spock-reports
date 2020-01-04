package com.athaydes.spockframework.report

import spock.lang.Ignore
import spock.lang.Issue
import spock.lang.Narrative
import spock.lang.PendingFeature
import spock.lang.See
import spock.lang.Specification
import spock.lang.Title
import spock.lang.Unroll

/**
 *
 * User: Renato
 */
@Title( 'This is just a Fake test to test spock-reports' )
@Narrative( """
As a user
I want foo
So that bar""" )
class FakeTest extends Specification {

    def "A first test"() {
        given:
        "we have x and y"

        and:
        "some more things"

        when:
        "I do crazy things"

        then:
        "I get one iteration pass and another fail"
        x == y

        where:
        "The examples below are used"
        x   | y
        'a' | 'a'
        'b' | 'c'

    }

    def "Another feature!!!!"() {
        setup:
        "Setup block here"
        expect:
        "Expecting something ??"
    }

    def "A when then spec"() {
        when:
        "This is the when"
        then:
        "This is the then"
    }

    @Ignore( "Feature not implemented yet" )
    def "Please ignore me"() {
        given:
        "Nothing"
        when:
        "Do nothing"
        then:
        "Nothing happens"
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
        verifyAll {
            3 == 2
            5 == 1
        }
    }

    def """An incredibly long feature description that unfortunately will popup in some cases where business
	analysts write these too detailed overviews of what the test should be all about when what they really
	should do is to let the details go in the body of the test using the Gherkin language which underlies BDD
	and is proven to make it easier for all involved to understand what the test is doing, what the inputs are
	and what the expected outcomes are in such a way that the best possible common understanding is reached"""() {
        expect:
        "The long description above to look good in the report"
    }

    def "A Spec with empty block Strings"() {
        given:
        def a = 0

        and:
        def b = 1

        when:
        def c = a + b

        then:
        ""
        c == 1

        and:
        "  "
        c > 0
    }

    @Unroll
    def "An @Unrolled spec with x=#x and y=#y"() {
        setup:
        "nothing"
        expect:
        "#x to be 0"
        x == 0
        and:
        "An error if y is 5"
        if ( y == 5 ) throw new RuntimeException( 'y is 5' )
        where:
        x | y
        0 | 1
        2 | 3
        0 | 5
    }

    @PendingFeature
    def "Future feature"() {
        when:
        'the feature is ready'
        then:
        'the annotation will be removed'
        throw new RuntimeException( 'Not ready' )
    }

}
