package com.athaydes.spockframework.report

import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

/**
 *
 * User: Renato
 */
class FakeTest extends Specification {

	def "A first test"( ) {
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

	def "Another feature!!!!"( ) {
		setup:
		"Setup block here"
		expect:
		"Expecting something ??"
	}

	def "A when then spec"( ) {
		when:
		"This is the when"
		then:
		"This is the then"
	}

	@Ignore
	def "Please ignore me"( ) {
		given:
		"Nothing"
		when:
		"Do nothing"
		then:
		"Nothing happens"
	}

	def "A test with an error"( ) {
		when:
		"An Exception is thrown"
		throw new RuntimeException( 'As expected' )

		then:
		"Will never succeed"
	}

	def "A test with a failure"( ) {
		when:
		"Do nothing"
		then:
		"Test fails"
		assert 3 == 2
	}

	def """An incredibly long feature description that unfortunately will popup in some cases where business
	analysts write these too detailed overviews of what the test should be all about when what they really
	should do is to let the details go in the body of the test using the Gherkin language which underlies BDD
	and is proven to make it easier for all involved to understand what the test is doing, what the inputs are
	and what the expected outcomes are in such a way that the best possible common understanding is reached"""( ) {
		expect:
		"The long description above to look good in the report"
	}

	def "A Spec with empty block Strings"( ) {
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
        "true"
        true
        where:
        x | y
        0 | 1
        2 | 3
    }

}
