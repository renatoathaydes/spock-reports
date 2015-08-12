package com.athaydes.spockframework.report.internal

import com.athaydes.spockframework.report.IReportCreator
import groovy.util.logging.Log
import org.spockframework.runtime.RunContext

import java.util.logging.Level

/**
 *
 * User: Renato
 */
@Log
class ConfigLoader {
    static final PROP_OUTPUT_DIR = 'com.athaydes.spockframework.report.outputDir'
    static final PROP_HIDE_EMPTY_BLOCKS = 'com.athaydes.spockframework.report.hideEmptyBlocks'

    final CUSTOM_CONFIG = "META-INF/services/${IReportCreator.class.name}.properties"

    Properties loadConfig() {
        def props = loadCustomProperties( loadDefaultProperties() )
        [ IReportCreator.class.name, PROP_OUTPUT_DIR, PROP_HIDE_EMPTY_BLOCKS ].each {
            def sysVal = System.properties[ it ]
            if ( sysVal ) {
                log.info( "Overriding property [$it] with System property's value: $sysVal" )
                props[ it ] = sysVal
            }
        }
        log.info( "SpockReports config loaded: $props" )
        props
    }

    Properties loadDefaultProperties() {
        def defaultProperties = new Properties()
        ConfigLoader.class.getResource( 'config.properties' )?.withInputStream {
            defaultProperties.load it
        }
        defaultProperties
    }

    Properties loadCustomProperties( Properties properties ) {
        def resources = RunContext.classLoader.getResources( CUSTOM_CONFIG )
        for ( URL url in resources ) {
            try {
                log.info( "Trying to load custom configuration at $url" )
                url.withInputStream { properties.load it }
            } catch ( IOException | IllegalArgumentException e ) {
                log.log( Level.FINE, "Unable to read config from ${url.path}", e )
            }
        }
        properties
    }
}
