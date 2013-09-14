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

	def "Time amounts should look adequate for Test Reports"( ) {
		given:
		"The example evaluates to a number"
		def example = Eval.me timeDurationInMillis

		when:
		"I Convert the time duration to a presentable String"
		def result = new StringFormatHelper().toTimeDuration( example )

		then:
		"The result is as expected"
		result == expected

		where:
		timeDurationInMillis                         | expected
		'0'                                          | '0'
		'1'                                          | '0.001 seconds'
		'250'                                        | '0.250 seconds'
		'1000'                                       | '1.000 seconds'
		'''2 + //ms
			4 * 1000 + // sec
			5 * 1000 * 60 + // min
			8 * 1000 * 60 * 60 // hour''' | '8 hours, 5 minutes, 4.002 seconds'
	}

	def "A formatted String should be converted nicely to an equivalent HTML String"( ) {
		when:
		"An a formatted String is converted to an HTML String"
		def result = new StringFormatHelper().formatToHtml( formattedString )

		then:
		"The result is as expected"
		result == expected

		where:
		formattedString | expected
		''              | ''
		'abc'           | 'abc'
		'Hi\tthere'     | 'Hi<br/>there'
		'Hi\nHo'        | 'Hi<br/>Ho'
	}

	def "A date should look as specified in the examples when shown in a report"( ) {
		given:
		"The dateParams in the examples are converted to a Gregorian Calendar Date"
		def date = new GregorianCalendar( * Eval.me( dateParams ) ).time

		when:
		"A Date is converted to a String"
		def result = new StringFormatHelper().toDateString( date )

		then:
		"The result is as in the examples"
		result.split( ' ' ).toList().containsAll( expectedStringsInResult )

		where:
		dateParams                                      | expectedStringsInResult
		'[ 1995, Calendar.SEPTEMBER, 5, 19, 35, 30 ]' | [ 'Tue', 'Sep', '05', '19:35:30', '1995' ]
		'[ 2013, Calendar.JANUARY, 31, 0, 0, 0 ]'     | [ 'Thu', 'Jan', '31', '00:00:00', '2013' ]
	}


}
