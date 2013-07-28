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

	Properties loadCustomProperties( Properties defaults ) {
		def resources = RunContext.classLoader.getResources( CUSTOM_CONFIG )
		def props = new Properties( defaults )
		Collections.list( resources ).each { URL url ->
			try {
				url.withInputStream { props.load it }
			} catch ( IOException | IllegalArgumentException e ) {
				println "Unable to read config from ${url.path}, $e"
			}
		}
		props
	}
}
