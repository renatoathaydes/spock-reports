package com.athaydes.spockframework.report.internal

import groovy.util.logging.Log

import java.util.logging.Level

import org.spockframework.runtime.RunContext

import com.athaydes.spockframework.report.IReportCreator

/**
 *
 * User: Renato
 */
@Log
class ConfigLoader {

	final CUSTOM_CONFIG = "META-INF/services/${IReportCreator.class.name}.properties"

	Properties loadConfig( ) {
		loadCustomProperties( loadDefaultProperties() )
	}

	Properties loadDefaultProperties( ) {
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
				url.withInputStream { properties.load it }
			} catch ( IOException | IllegalArgumentException e ) {
				log.log(Level.FINE, "Unable to read config from ${url.path}", e)
			}
		}
		properties
	}
}
