package com.athaydes.spockframework.report

import com.athaydes.spockframework.report.internal.HtmlReportCreator
import com.athaydes.spockframework.report.internal.SpecData
import com.athaydes.spockframework.report.internal.StringFormatHelper
import groovy.text.SimpleTemplateEngine
import org.junit.runner.notification.RunNotifier
import org.spockframework.runtime.Sputnik
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
				executedFeatures: 4,
				failures: 1,
				errors: 1,
				skipped: 1,
				successRate: '50.0%',
				time: UNKNOWN,
		]
		def templateEngine = new SimpleTemplateEngine()
		try {
			templateEngine.createTemplate( rawHtml ).make( binding ).toString()
		} catch ( e ) { e.printStackTrace() }
	}

	private String defaultStyle( ) {
		HtmlReportCreator.class.getResource( 'report.css' ).text
	}

	@Category( StringFormatHelper )
	class PredictableTimeResponse {
		String totalTime( SpecData d ) { JUnitReportExtensionSpec.UNKNOWN }
	}
}
