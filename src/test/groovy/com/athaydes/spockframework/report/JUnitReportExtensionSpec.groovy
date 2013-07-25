package com.athaydes.spockframework.report

import com.athaydes.spockframework.report.internal.TestHelper
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
		def expectedHtml = this.class.getResource( 'FakeTestReport.html' )
				.text.replaceAll( '\t', '' )

		expect:
		"A nice HTML report to have been generated under the build directory"
		def reportFile = Paths.get( buildDir, 'spock-reports',
				FakeTest.class.name + '.html' ).toFile()
		reportFile.exists()

		and:
		"The contents are functionally the same as expected"
		minify( reportFile.text ) == minify( expectedHtml )

	}

}
