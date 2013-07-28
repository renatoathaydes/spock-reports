package com.athaydes.spockframework.report.internal

import spock.lang.Specification

/**
 *
 * User: Renato
 */
class ConfigLoaderSpec extends Specification {

	private static final String HTML_REPORT_CSS = HtmlReportCreator.class.name + '.css'

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
		result.getProperty( HTML_REPORT_CSS ) == 'report.css'
	}

	def "Custom configurations should override default configurations"( ) {
		given:
		"A ConfigLoader in an environment where there is a custom config file"
		def configLoader = new ConfigLoader()
		File customFile = createFile( configLoader.CUSTOM_CONFIG ).withContents( expected )

		and:
		"The configLocation exists"
		customFile.exists()

		when:
		"I ask the ConfigLoader to load the configuration"
		def result = configLoader.loadConfig()

		then:
		"The ConfigLoader to find all of the properties declared in the configLocation"
		result.getProperty( HTML_REPORT_CSS ) == expected

		cleanup:
		customFile.delete()

		where:
		expected << [ 'example/report.css' ]
	}

	private createFile( String fileName ) {
		def buildDir = System.getProperty( 'project.buildDir' )
		def file = new File( "${buildDir}/classes/test", fileName )
		new File( file.parent ).mkdirs()
		[ withContents: { expected -> file << "${HTML_REPORT_CSS}=${expected}" } ]
	}

}
