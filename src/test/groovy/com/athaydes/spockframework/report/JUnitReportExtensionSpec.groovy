package com.athaydes.spockframework.report

import com.athaydes.spockframework.report.internal.ConfigLoader
import com.athaydes.spockframework.report.internal.HtmlReportCreator
import com.athaydes.spockframework.report.internal.SpecData
import com.athaydes.spockframework.report.internal.StringFormatHelper
import groovy.text.SimpleTemplateEngine
import org.junit.runner.notification.RunNotifier
import org.spockframework.runtime.Sputnik
import org.spockframework.runtime.model.SpecInfo
import spock.lang.Specification

import java.nio.file.Paths

import static com.athaydes.spockframework.report.internal.TestHelper.minify

/**
 *
 * User: Renato
 */
class JUnitReportExtensionSpec extends Specification {

	static final String UNKNOWN = 'Unknown'

	def "A correct HTML report is generated for a spec including different types of features"( ) {
		given:
		"The project build folder location is known"
		def buildDir = System.getProperty( 'project.buildDir' )

		and:
		"The expected HTML report (not counting time fields)"
		String expectedHtml = expectedHtmlReport()

		and:
		"Method HtmlReportCreator.totalTime( SpecData ) is mocked out to fill time fields with known values"

		when:
		"A Specification containing different types of features is run by Spock"
		use( PredictableTimeResponse ) {
			new Sputnik( FakeTest ).run( new RunNotifier() )
		}

		then:
		"A nice HTML report to have been generated under the build directory"
		def reportFile = Paths.get( buildDir, 'spock-reports',
				FakeTest.class.name + '.html' ).toFile()
		reportFile.exists()

		and:
		"The contents are functionally the same as expected"
		minify( reportFile.text ) == minify( expectedHtml )
	}

	private String expectedHtmlReport( ) {
		def rawHtml = this.class.getResource( 'FakeTestReport.html' ).text
		def binding = [
				classOnTest: FakeTest.class.name,
				style: defaultStyle(),
				executedFeatures: 6,
				failures: 2,
				errors: 1,
				skipped: 1,
				successRate: '50.0%',
				time: UNKNOWN,
				projectUrl: JUnitReportExtension.PROJECT_URL
		]
		def templateEngine = new SimpleTemplateEngine()
		try {
			templateEngine.createTemplate( rawHtml ).make( binding ).toString()
		} catch ( e ) { e.printStackTrace(); null }
	}

	def "The settings found in the config.properties file are used to configure the report framework"( ) {
		given:
		"An instance of JUnitReportExtension with a mocked out config loader"
		def extension = new JUnitReportExtension()
		def props = new Properties()
		extension.configLoader = [ loadConfig: { props } ] as ConfigLoader

		and:
		"A set of valid and invalid properties emulating the properties file"
		props.setProperty( IReportCreator.class.name, MockReportCreator.class.name )
		props.setProperty( MockReportCreator.class.name + '.customProp', 'customValue' )
		props.setProperty( "com.athaydes.spockframework.report.outputDir", "the-output-dir" )
		props.setProperty( "some.invalid.property", "invalid-value" )

		when:
		"This extension framework is initiated by Spock visiting a spec"
		def listenersAdded = 0
		extension.visitSpec( [ addListener: { SpecInfoListener _ ->
			listenersAdded++
		} ] as SpecInfo )

		then:
		"This extension added a SpecInfoListener to Spock's SpecInfo"
		listenersAdded == 1

		and:
		"The ReportCreator was configured with the valid properties"
		MockReportCreator.outputDirs == [ "the-output-dir" ]
		MockReportCreator.customPropValues == [ 'customValue' ]

		cleanup:
		MockReportCreator.outputDirs = null
		MockReportCreator.customPropValues = null

	}

	private String defaultStyle( ) {
		HtmlReportCreator.class.getResource( 'report.css' ).text
	}

	@Category( StringFormatHelper )
	class PredictableTimeResponse {
		String totalTime( SpecData d ) { JUnitReportExtensionSpec.UNKNOWN }
	}

	class MockReportCreator implements IReportCreator {

		static List<String> outputDirs = [ ]
		static List<String> customPropValues = [ ]

		@Override
		void createReportFor( SpecData data ) {}

		@Override
		void setOutputDir( String path ) { outputDirs << path }

		void setCustomProp( String value ) { customPropValues << value }
	}

}
