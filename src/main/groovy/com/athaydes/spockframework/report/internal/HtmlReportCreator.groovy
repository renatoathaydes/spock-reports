package com.athaydes.spockframework.report.internal


import com.athaydes.spockframework.report.IReportCreator

/**
 *
 * User: Renato
 */
class HtmlReportCreator implements IReportCreator {

	def reportAggregator = new HtmlReportAggregator()

	@Override
	void createReportFor( SpecData data ) {

		//TODO
		println """
				|*************************************
				|Output: ${System.getProperties().propertyNames().collect()}
				||File: ${new File( '.' ).absolutePath}
				|Spec Info: ${data.info}
				|Errors By Feature: ${data.featureRuns*.errorsByIteration}
				|*************************************""".stripMargin()
	}

}
