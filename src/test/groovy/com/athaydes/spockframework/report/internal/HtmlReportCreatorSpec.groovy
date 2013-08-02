package com.athaydes.spockframework.report.internal

import com.athaydes.spockframework.report.FakeTest
import com.athaydes.spockframework.report.SpockReportExtension
import groovy.text.SimpleTemplateEngine
import groovy.xml.MarkupBuilder
import org.junit.runner.notification.RunNotifier
import org.spockframework.runtime.Sputnik
import spock.lang.Specification

import java.nio.file.Paths

import static com.athaydes.spockframework.report.internal.TestHelper.minify

/**
 *
 * User: Renato
 */
class HtmlReportCreatorSpec extends Specification {

	static final String UNKNOWN = 'Unknown'

	def "A correct HTML report is generated for a spec including different types of features"( ) {
		given:
		"The project build folder location is known"
		def buildDir = System.getProperty( 'project.buildDir', 'build' )

		and:
		"The expected HTML report (not counting time fields and errors)"
		String expectedHtml = expectedHtmlReport()

		and:
		"Method HtmlReportCreator.totalTime( SpecData ) is mocked out to fill time fields with known values"
		// using PredictableTimeResponse category to mock that

		and:
		"ProblemBlockWriter is mocked out to fill error blocks with known contents"
		// using PredictableProblems category to mock that

		when:
		"A Specification containing different types of features is run by Spock"
		use( PredictableProblems, PredictableTimeResponse ) {
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
		def rawHtml = HtmlReportCreator.getResource( 'FakeTestReport.html' ).text
		def binding = [
				classOnTest: FakeTest.class.name,
				style: defaultStyle(),
				executedFeatures: 6,
				failures: 2,
				errors: 1,
				skipped: 1,
				successRate: '50.0%',
				problemList1: UNKNOWN,
				problemList2: UNKNOWN,
				problemList3: UNKNOWN,
				time: UNKNOWN,
				projectUrl: SpockReportExtension.PROJECT_URL
		]
		def templateEngine = new SimpleTemplateEngine()
		try {
			templateEngine.createTemplate( rawHtml ).make( binding ).toString()
		} catch ( e ) { e.printStackTrace(); null }
	}

	private String defaultStyle( ) {
		this.class.getResource( 'report.css' ).text
	}

	@Category( StringFormatHelper )
	class PredictableTimeResponse {
		String toTimeDuration( timeInMs ) { HtmlReportCreatorSpec.UNKNOWN }
	}

	@Category( ProblemBlockWriter )
	class PredictableProblems {
		void writeProblems( MarkupBuilder builder, FeatureRun run, boolean isError ) {
			builder.mkp.yieldUnescaped( HtmlReportCreatorSpec.UNKNOWN )
		}
	}

}
