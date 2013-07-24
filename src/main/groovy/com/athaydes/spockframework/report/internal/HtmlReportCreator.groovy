package com.athaydes.spockframework.report.internal


import com.athaydes.spockframework.report.IReportCreator

import java.nio.file.Paths

/**
 *
 * User: Renato
 */
class HtmlReportCreator implements IReportCreator {

	def reportAggregator = new HtmlReportAggregator()

	@Override
	void createReportFor( SpecData data ) {
		def specClassName = data.info.description.className
		def reportsDir = new File( 'build', 'spock-reports' )
		reportsDir.mkdirs()
		Paths.get( reportsDir.absolutePath, specClassName + '.html' )
				.toFile().write( reportFor( data ) )
	}

	String reportFor( SpecData data ) {
		"""<html>
		<head>
		</head>
		<body>
			<h1>Report for ${data.info.description.className}</h1>
		</body>
		</html>""".replaceAll( '\t', '' )
	}

}
