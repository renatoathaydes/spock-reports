package com.athaydes.spockframework.report.internal

import groovy.time.TimeDuration

/**
 *
 * User: Renato
 */
class StringFormatHelper {


	String totalTime( SpecData data ) {
		new TimeDuration( 0, 0, 0, data.totalTime.toInteger() ).toString()
	}

	def toPercentage( double rate ) {
		String.format( '%.2f%%', rate * 100 ).replace( '.00', '.0' )
	}

}
