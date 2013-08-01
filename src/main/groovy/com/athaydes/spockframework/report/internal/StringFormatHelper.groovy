package com.athaydes.spockframework.report.internal

import groovy.time.TimeDuration

/**
 *
 * User: Renato
 */
class StringFormatHelper {

	private final MINUTE = 60 * 1000
	private final HOUR = 60 * MINUTE

	String toTimeDuration( timeInMs ) {
		long t = timeInMs.toLong()
		int hours = ( t / HOUR ).toInteger()
		int mins = ( ( t - HOUR * hours ) / MINUTE ).toInteger()
		int secs = ( ( t - HOUR * hours - mins * MINUTE ) / 1000 ).toInteger()
		int millis = ( t % 1000 ).toInteger()
		new TimeDuration( hours, mins, secs, millis ).toString()
	}

	String toPercentage( double rate ) {
		String.format( '%.2f%%', rate * 100 ).replace( '.00', '.0' )
	}

	String formatToHtml( String text ) {
		text.replaceAll( /[\t\n]/, '<br/>' )
	}

}
