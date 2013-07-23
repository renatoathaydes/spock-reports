package org.spockframework.extension

import spock.lang.Specification

/**
 *
 * User: Renato
 */
class JUnitReportExtensionSpec extends Specification {

	def "A test number 1"( ) {
		given:
		"we have x and y"

		and:
		"some more things"

		when:
		"I do crazy things"

		then:
		"I get a lot of stuff"
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

}
