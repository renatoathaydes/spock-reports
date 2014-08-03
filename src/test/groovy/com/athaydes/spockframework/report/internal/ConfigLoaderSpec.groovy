package com.athaydes.spockframework.report.internal

import com.athaydes.spockframework.report.IReportCreator
import spock.lang.Specification

/**
 *
 * User: Renato
 */
class ConfigLoaderSpec extends Specification {

	private static final String FEATURE_REPORT_CSS = HtmlReportCreator.class.name + '.featureReportCss'

	def "The ConfigLoader should load the default configurations"( ) {
		given:
		"A ConfigLoader without any custom configuration"
		def configLoader = new ConfigLoader()

		and:
		"The configLocation exists"
		( configLoader.CUSTOM_CONFIG as File ).exists()

		when:
		"I ask the ConfigLoader to load the configuration"
		def result = configLoader.loadConfig()

		then:
		"The ConfigLoader to find all of the properties declared in the configLocation"
		result.getProperty( FEATURE_REPORT_CSS ) == 'spock-feature-report.css'
		result.getProperty( 'com.athaydes.spockframework.report.hideEmptyBlocks' ) == 'false'
	}

	def "Custom configurations should override default configurations"( ) {
		given:
		"A ConfigLoader in an environment where there is a custom config file"
		def configLoader = new ConfigLoader()
		File customFile = createFileUnderMetaInf( IReportCreator.class.name + '.properties' )
		customFile.write "${FEATURE_REPORT_CSS}=${expected}"

		and:
		"The configLocation exists"
		assert customFile.exists()

		when:
		"I ask the ConfigLoader to load the configuration"
		def result = configLoader.loadConfig()

		then:
		"The ConfigLoader to find all of the properties declared in the configLocation"
		result.getProperty( FEATURE_REPORT_CSS ) == expected

		and:
		"The default properties are also kept"
		result.getProperty( 'com.athaydes.spockframework.report.hideEmptyBlocks' ) == 'false'
		result.getProperty( 'com.athaydes.spockframework.report.outputDir' ) == 'build/spock-reports'

		cleanup:
		assert customFile.delete()

		where:
		expected << [ 'example/report.css' ]
	}

	private createFileUnderMetaInf( String fileName ) {
		def globalExtConfig = this.class.getResource( '/META-INF/services/org.spockframework.runtime.extension.IGlobalExtension' )
		def f = new File( globalExtConfig.toURI() )
		new File( f.parentFile, fileName )
	}

}
