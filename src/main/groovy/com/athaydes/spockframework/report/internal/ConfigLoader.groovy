package com.athaydes.spockframework.report.internal

import org.spockframework.runtime.RunContext

import com.athaydes.spockframework.report.IReportCreator

/**
 *
 * User: Renato
 */
class ConfigLoader {
	static final PROP_OUTPUT_DIR = 'com.athaydes.spockframework.report.outputDir'
	static final PROP_HIDE_EMPTY_BLOCKS = 'com.athaydes.spockframework.report.hideEmptyBlocks'

	final CUSTOM_CONFIG = "META-INF/services/${IReportCreator.class.name}.properties"
	
	Properties loadConfig( ) {
		def props = loadCustomProperties( loadDefaultProperties() )
		[IReportCreator.class.name, PROP_OUTPUT_DIR, PROP_HIDE_EMPTY_BLOCKS].each {
			def sysVal = System.properties[it]
			if(sysVal)
				props[it] = sysVal
		}
		props
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
				println "Unable to read config from ${url.path}, $e"
			}
		}
		properties
	}
}
