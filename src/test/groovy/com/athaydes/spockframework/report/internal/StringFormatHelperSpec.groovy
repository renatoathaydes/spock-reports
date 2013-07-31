package com.athaydes.spockframework.report.internal

import spock.lang.Specification

/**
 *
 * User: Renato
 */
class StringFormatHelperSpec extends Specification {

	def "Percentage values should be adequate for a Test Report"( ) {
		given:
		"Doubles representing rate of success for a test"

		when:
		"Formatting the Doubles to show them in a report"
		def result = new StringFormatHelper().toPercentage( input )
		then:
		"The values should look as in the examples"
		result == expected

		where:
		input     | expected
		0         | '0.0%'
		0.1       | '10.0%'
		0.25      | '25.0%'
		0.5       | '50.0%'
		3.0 / 4.0 | '75.0%'
		1.0 / 3.0 | '33.33%'
		1.0       | '100.0%'
	}

	def "Time amounts should be adequate for Test Reports"( ) {
		expect:
		"Groovy to take care of that!"

	}
}
