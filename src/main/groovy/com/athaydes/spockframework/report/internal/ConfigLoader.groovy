package com.athaydes.spockframework.report.internal

import com.athaydes.spockframework.report.IReportCreator
import groovy.util.logging.Slf4j
import org.spockframework.runtime.RunContext

/**
 *
 * User: Renato
 */
@Slf4j
class ConfigLoader {
    static final PROP_OUTPUT_DIR = 'com.athaydes.spockframework.report.outputDir'
    static final PROP_HIDE_EMPTY_BLOCKS = 'com.athaydes.spockframework.report.hideEmptyBlocks'
    static final PROP_SHOW_CODE_BLOCKS = 'com.athaydes.spockframework.report.showCodeBlocks'
    static final PROP_TEST_SOURCE_ROOTS = 'com.athaydes.spockframework.report.testSourceRoots'
    static final PROP_PROJECT_NAME = 'com.athaydes.spockframework.report.projectName'
    static final PROP_PROJECT_VERSION = 'com.athaydes.spockframework.report.projectVersion'

    final CUSTOM_CONFIG = "META-INF/services/${IReportCreator.class.name}.properties"

    Properties loadConfig() {
        def props = loadSystemProperties(
                loadCustomProperties(
                        loadDefaultProperties() ) )

        log.info( "SpockReports config loaded: {}", props )

        props
    }

    boolean getBoolean( String key, Properties props ) {
        try {
            return Boolean.parseBoolean( props.getProperty( key ) )
        } catch ( e ) {
            log.warn( "Invalid value for ${key}. Should be true or false. Error: $e" )
            return false
        }
    }

    private Properties loadSystemProperties( Properties props ) {
        [ IReportCreator.name, PROP_OUTPUT_DIR, PROP_HIDE_EMPTY_BLOCKS, PROP_SHOW_CODE_BLOCKS,
          PROP_TEST_SOURCE_ROOTS, PROP_PROJECT_NAME, PROP_PROJECT_VERSION ].each {
            def sysVal = System.properties[ it ]
            if ( sysVal ) {
                log.debug( "Overriding property [$it] with System property's value: $sysVal" )
                props[ it ] = sysVal
            }
        }
        props
    }

    private Properties loadDefaultProperties() {
        def defaultProperties = new Properties()
        ConfigLoader.class.getResource( 'config.properties' )?.withInputStream {
            defaultProperties.load it
        }
        defaultProperties
    }

    private Properties loadCustomProperties( Properties properties ) {
        def resources = RunContext.classLoader.getResources( CUSTOM_CONFIG )
        for ( URL url in resources ) {
            try {
                log.debug( "Trying to load custom configuration at $url" )
                url.withInputStream { properties.load it }
            } catch ( IOException | IllegalArgumentException e ) {
                log.warn( "Unable to read config from ${url.path}", e )
            }
        }
        properties
    }
}
