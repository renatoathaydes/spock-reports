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

	def "The settings found in the config.properties file are read only once"( ) {
		given:
		"An instance of JUnitReportExtension with a mocked out config loader"
		def extension = new JUnitReportExtension()
		def callsCount = 0
		extension.configLoader = [ loadConfig: { callsCount++; new Properties() } ] as ConfigLoader

		when:
		"Spock visits one spec"
		extension.visitSpec( Mock( SpecInfo ) )

		then:
		"The config is read once"
		callsCount == 1

		when:
		"Spock visits another 10 specs"
		10.times { extension.visitSpec( Mock( SpecInfo ) ) }

		then:
		"The config has still been read only once"
		callsCount == 1
	}

	def "The settings found in the config.properties file are used to configure the report framework"( ) {
		given:
		"A mock ReportCreator"
		def mockReportCreator = GroovyMock( IReportCreator )

		and:
		"A mock ConfigLoader"
		def mockConfigLoader = Mock( ConfigLoader )
		def props = new Properties()
		mockConfigLoader.loadConfig() >> props

		and:
		"A mock Spec"
		def mockSpecInfo = Mock( SpecInfo )

		and:
		"An instance of JUnitReportExtension with mocked out config loader and reportCreator"
		def extension = new JUnitReportExtension() {
			@Override
			def instantiateReportCreator( ) { mockReportCreator }
		}
		extension.configLoader = mockConfigLoader

		and:
		"A set of valid and invalid properties emulating the properties file"
		props.setProperty( IReportCreator.class.name, "MockReportCreator" )
		props.setProperty( "MockReportCreator" + '.customProp', 'customValue' )
		props.setProperty( "com.athaydes.spockframework.report.outputDir", "the-output-dir" )
		props.setProperty( "some.invalid.property", "invalid-value" )

		when:
		"This extension framework is initiated by Spock visiting the mock spec"
		extension.visitSpec( mockSpecInfo )

		then:
		"The class of the implementation of ReportCreator was correctly set"
		extension.reportCreatorClassName == "MockReportCreator"

		and:
		"This extension added a SpecInfoListener to Spock's SpecInfo"
		1 * mockSpecInfo.addListener( _ )

		and:
		"The ReportCreator was configured with the valid properties"
		1 * mockReportCreator.setOutputDir( "the-output-dir" )
		1 * mockReportCreator.setCustomProp( "customValue" )
	}

	private String defaultStyle( ) {
		HtmlReportCreator.class.getResource( 'report.css' ).text
	}

	@Category( StringFormatHelper )
	class PredictableTimeResponse {
		String totalTime( SpecData d ) { JUnitReportExtensionSpec.UNKNOWN }
	}

}
