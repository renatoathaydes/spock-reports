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

    static final String SYS_PROPERTY_PREFIX = 'com.athaydes.spockframework.report.'

    static final CUSTOM_CONFIG = "META-INF/services/${IReportCreator.class.name}.properties"

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

    void apply( IReportCreator reportCreator, Properties config ) {

        config.each { String key, value ->
            int lastDotIndex = key.lastIndexOf( '.' )

            if ( lastDotIndex > 0 && lastDotIndex + 1 < key.size() ) {
                final prefix = key[ 0..<lastDotIndex ]
                final propertyName = key[ ( lastDotIndex + 1 )..-1 ]

                if ( prefix == IReportCreator.package.name || prefix == reportCreator.class.name ) {
                    if ( reportCreator.hasProperty( propertyName ) ) {
                        try {
                            reportCreator."$propertyName" = value
                            log.debug( "Property $propertyName set to $value" )
                        } catch ( ignore ) {
                            log.warn( "Invalid property value for property '{}'", propertyName )
                        }
                    } else {
                        log.warn( "Property [{}] not acceptable by IReportCreator of type {}",
                                propertyName, reportCreator.class.name )
                    }
                } else {
                    log.debug( "Ignoring property '{}' for IReportCreator of type {}",
                            propertyName, reportCreator.class.name )
                }
            }
        }
    }

    private Properties loadSystemProperties( Properties props ) {
        def filteredProps = System.properties.findAll { entry ->
            def key = entry.key
            key instanceof String && key.startsWith( SYS_PROPERTY_PREFIX )
        }

        def reportClassName = props[ IReportCreator.name ]

        // add also the properties starting with the report's class name
        if ( reportClassName instanceof String ) {
            def reportClassPrefix = reportClassName + '.'
            System.properties.findAll { entry ->
                def key = entry.key
                key instanceof String && key.startsWith( reportClassPrefix )
            }.each { String key, value ->
                filteredProps[ key ] = value
            }
        }

        filteredProps.each { key, value ->
            if ( props.containsKey( key ) ) {
                log.debug( "Overriding property [$key] with System property's value: $value" )
            }
            props[ key ] = value
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
