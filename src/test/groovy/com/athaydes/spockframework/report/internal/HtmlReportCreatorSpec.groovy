package com.athaydes.spockframework.report.internal

import com.athaydes.spockframework.report.FakeTest
import com.athaydes.spockframework.report.SpockReportExtension
import groovy.text.SimpleTemplateEngine
import groovy.xml.MarkupBuilder
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

	def "The css file used should be loaded correctly from any file in the classpath"( ) {
		given:
		"A css file in the classpath with known contents"
		def reportFile = new File( this.class.getResource( "${this.class.simpleName}.class" ).toURI() )
		def cssFile = new File( reportFile.parentFile, 'my.css' ) << 'file-contents'

		when:
		"The css is set to the file location relative to the classpath"
		def reportCreator = new HtmlReportCreator( css:
				this.class.package.name.replace( '.', '/' ) + '/my.css' )

		then:
		"The css property of the report becomes the contents of the css file"
		reportCreator.css == 'file-contents'

		cleanup:
		cssFile?.delete()
	}

	def "The report aggregator should be given the required information for each spec visited"( ) {
		given:
		"A HtmlReportCreator with mocked out stats() method"
		def mockedStats = [ failures: 9, errors: 8, skipped: 7, totalRuns: 50, successRate: 0.1 ]
		def reportCreator = new HtmlReportCreator() {
			protected Map stats( SpecData data ) { mockedStats }
		}

		and:
		"The HtmlReportCreator has a known outputDir"
		reportCreator.outputDir = 'outputDir'

		and:
		"A mock report aggregator, a stubbed SpecData and an injected MarkupBuilder"
		reportCreator.reportAggregator = Mock( HtmlReportAggregator )
		def stubSpecData = Stub( SpecData )
		def stubInfo = Stub( SpecInfo )
		stubSpecData.info >> stubInfo
		stubInfo.name >> 'some-name'
		def builder = new MarkupBuilder( Stub( Writer ) )

		when:
		"The report creator writes the summary report"
		reportCreator.writeSummary( builder, stubSpecData )

		then:
		"The stats shown in the summary report and the outputDir are passed on to the mock report aggregator"
		1 * reportCreator.reportAggregator.aggregateReport( 'some-name', mockedStats, 'outputDir' )

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
		this.class.getResource( '/report.css' ).text
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
