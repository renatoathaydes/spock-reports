package com.athaydes.spockframework.report

import spock.lang.Ignore
import spock.lang.Specification

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

}
