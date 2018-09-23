package com.athaydes.spockframework.report.internal

import groovy.time.TimeDuration

import java.text.DecimalFormat

/**
 *
 * User: Renato
 */
class StringFormatHelper {

    private final MINUTE = 60 * 1000
    private final HOUR = 60 * MINUTE
    static final ds = new DecimalFormat().decimalFormatSymbols.decimalSeparator

    String toTimeDuration( timeInMs ) {
        long t = timeInMs?.toLong() ?: 0L
        int hours = ( t / HOUR ).toInteger()
        int mins = ( ( t - HOUR * hours ) / MINUTE ).toInteger()
        int secs = ( ( t - HOUR * hours - mins * MINUTE ) / 1000 ).toInteger()
        int millis = ( t % 1000 ).toInteger()
        internationalizeTimeDuration( new TimeDuration( hours, mins, secs, millis ) )
    }

    private String internationalizeTimeDuration( TimeDuration timeDuration ) {
        ( ds == '.' ) ? timeDuration.toString() : timeDuration.toString().replace( '.', ds.toString() )
    }

    String toPercentage( double rate ) {
        String.format( '%.2f%%', rate * 100 ).replace( "${ds}00", "${ds}0" )
    }

    String formatToHtml( String text ) {
        text.replaceAll( /[\t\n]/, '<br/>' )
    }

    String toDateString( Date date ) {
        date.toString()
    }

    String escapeXml( String str ) {
        str
                .replaceAll( '&', '&amp;' )
                .replaceAll( '<', '&lt;' )
                .replaceAll( '>', '&gt;' )
                .replaceAll( '"', '&quot;' )
                .replaceAll( '\'', '&apos;' )
    }
}
