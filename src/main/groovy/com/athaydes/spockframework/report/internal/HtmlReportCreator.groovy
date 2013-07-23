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
				.toFile() << '<html>report goes here</html>'
	}

}
