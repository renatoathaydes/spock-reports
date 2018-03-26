package com.athaydes.spockframework.report.internal

import groovy.transform.ToString
import spock.config.ConfigurationObject

@ConfigurationObject( 'spockReports' )
@ToString
class SpockReportsConfiguration {

    Map<String, Object> properties = [ : ]

    void addSet( map ) {
        if ( map instanceof Map ) {
            map.each { k, value ->
                properties[ k.toString() ] = value
            }
        } else {
            throw new IllegalArgumentException( "Expected map entry (e.g. 'set \"some.property\": \"value\"'), found $map" )
        }
    }

}
