package com.athaydes.spockframework.report

import org.junit.runner.notification.RunNotifier
import org.spockframework.runtime.Sputnik
import spock.lang.Specification

import java.nio.file.Paths

/**
 *
 * User: Renato
 */
class JUnitReportExtensionSpec extends Specification {

	final expectedReportForFakeTest = """<html>
		<head>
		</head>
		<body>
			<h1>Report for ${FakeTest.name}</h1>
		</body>
		</html>""".replaceAll( '\t', '' )

	def "A correct HTML report is generated for a spec including different types of features"( ) {
		given:
		"A Specification containing different types of features is run by Spock"
		new Sputnik( FakeTest ).run( new RunNotifier() )

		and:
		"The build folder location is known"
		def buildDir = System.getProperty( 'project.buildDir' )

		expect:
		"A nice HTML report to have been generated under the build directory"
		def reportFile = Paths.get( buildDir, 'spock-reports',
				FakeTest.class.name + '.html' ).toFile()
		reportFile.exists()
		reportFile.text == expectedReportForFakeTest

	}

}
