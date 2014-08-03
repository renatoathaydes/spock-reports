package com.athaydes.spockframework.report.internal

import com.athaydes.spockframework.report.IReportCreator
import org.spockframework.runtime.RunContext

/**
 *
 * User: Renato
 */
class ConfigLoader {

	final CUSTOM_CONFIG = "META-INF/services/${IReportCreator.class.name}.properties"

	Properties loadConfig( ) {
		loadCustomProperties( loadDefaultProperties() )
	}

	Properties loadDefaultProperties( ) {
		def defaultProperties = new Properties()
		ConfigLoader.class.getResource( 'config.properties' ).withInputStream {
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
				println "Unable to read config from ${url.path}, $e"
			}
		}
		properties
	}
}
