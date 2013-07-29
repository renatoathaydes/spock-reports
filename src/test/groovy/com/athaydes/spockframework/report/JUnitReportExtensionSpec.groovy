package com.athaydes.spockframework.report

import com.athaydes.spockframework.report.internal.HtmlReportCreator
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

	def "A correct HTML report is generated for a spec including different types of features"( ) {
		given:
		"A Specification containing different types of features is run by Spock"
		new Sputnik( FakeTest ).run( new RunNotifier() )

		and:
		"The build folder location is known"
		def buildDir = System.getProperty( 'project.buildDir' )

		and:
		"The expected HTML report"
		String expectedHtml = expectedHtmlReport()


		expect:
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
				executedTests: 4,
				failures: 1,
				skipped: 1
		]
		def templateEngine = new SimpleTemplateEngine()
		templateEngine.createTemplate( rawHtml ).make( binding ).toString()
	}

	private String defaultStyle( ) {
		HtmlReportCreator.class.getResource( 'report.css' ).text
	}

}
