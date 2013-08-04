package com.athaydes.spockframework.report.internal

import groovy.xml.MarkupBuilder
import spock.lang.Specification

import static com.athaydes.spockframework.report.internal.TestHelper.minify

/**
 *
 * User: Renato
 */
class HtmlReportAggregatorSpec extends Specification {

	def "The aggregate data for a sequence of test results should be computed correctly"( ) {
		given:
		"An HtmlReportAggregator with known aggregatedData"
		def aggregator = new HtmlReportAggregator()
		aggregator.aggregatedData << aggregatedData

		when:
		"I request the aggregate data to be recomputed"
		def result = aggregator.recomputeAggregateData()

		then:
		"The expected result is obtained"
		result == expected

		where:
		aggregatedData | expected
		[
				'A': [ failures: 1, errors: 2,
						skipped: 0, totalRuns: 10,
						successRate: 0.7, time: 1.25 ],
				'B': [ failures: 6, errors: 4,
						skipped: 0, totalRuns: 20,
						successRate: 0.5, time: 2.0 ],
				'C': [ failures: 0, errors: 0,
						skipped: 0, totalRuns: 1,
						successRate: 1.0, time: 3.0 ]
		]              | [ total: 3, passed: 1, failed: 2, fFails: 7, fErrors: 6, time: 6.25 ]
	}

	def """When a single spec data is provided to the HtmlReportAggregator it
           should create a report with data from the single spec"""( ) {
		given:
		"A single spec stats"
		def stats = [ failures: 1, errors: 0, skipped: 2, totalRuns: 5, successRate: 0.25, time: 0 ]

		and:
		"An output directory"
		def outputDir = "build/${this.class.simpleName}"

		and:
		"A HtmlReportAggregator with a mocked out StringFormatHelper and writeFooter() method"
		def mockStringFormatter = Stub( StringFormatHelper )
		mockStringFormatter.toPercentage( _ ) >>> [ '25.0%', '25%' ]
		mockStringFormatter.toTimeDuration( _ ) >>> [ '1.0 second', '1 sec' ]
		def aggregator = new HtmlReportAggregator() {
			protected void writeFooter( MarkupBuilder builder ) {
				builder.div( 'class': 'footer', 'The footer' )
			}
		}
		aggregator.stringFormatter = mockStringFormatter

		when:
		"The spec data is provided to the HtmlReportAggregator"
		aggregator.aggregateReport( 'Spec1', stats, outputDir )
		def reportFile = new File( outputDir, 'index.html' )

		then:
		"An aggregated report called index.html should be created in the outputDir"
		reportFile.exists()

		and:
		"The contents are functionally the same as expected"
		minify( reportFile.text ) == minify( singleTestSummaryExpectedHtml() )
	}

	def """When several specs data are provided to the HtmlReportAggregator it
           should create a report aggregating all of the individual spec reports"""( ) {
		given:
		"Several specs"
		def allSpecs = [
				[ 'Spec1': [ failures: 1, errors: 0, skipped: 2, totalRuns: 5, successRate: 0.1 ] ],
				[ 'a.Spec2': [ failures: 2, errors: 1, skipped: 3, totalRuns: 6, successRate: 0.2 ] ],
				[ 'a.Spec3': [ failures: 3, errors: 2, skipped: 4, totalRuns: 7, successRate: 0.3 ] ],
				[ 'a.b.c.Spec4': [ failures: 4, errors: 3, skipped: 5, totalRuns: 8, successRate: 0.4 ] ],
				[ 'b.c.Spec6': [ failures: 6, errors: 5, skipped: 7, totalRuns: 10, successRate: 0.6 ] ],
				[ 'a.b.c.Spec5': [ failures: 5, errors: 4, skipped: 6, totalRuns: 9, successRate: 0.5 ] ],
				[ 'c.d.Spec6': [ failures: 7, errors: 6, skipped: 8, totalRuns: 11, successRate: 0.7 ] ]
		]

		and:
		"An output directory"
		def outputDir = "build/${this.class.simpleName}"

		and:
		"A HtmlReportAggregator"
		def aggregator = new HtmlReportAggregator()

		when:
		"The specs data is provided to the HtmlReportAggregator"
		allSpecs.each { String name, Map stats ->
			aggregator.aggregateReport( name, stats, outputDir )
		}
		def reportFile = new File( outputDir, 'index.html' )

		then:
		"An aggregated report called index.html should be created in the outputDir"
		reportFile.exists()

		and:
		"The contents are functionally the same as expected"
		minify( reportFile.text ) == minify( testSummaryExpectedHtml() )
	}

	private String testSummaryExpectedHtml( ) {
		this.class.getResource( 'TestSummaryReport.html' ).text
	}

	private String singleTestSummaryExpectedHtml( ) {
		this.class.getResource( 'SingleTestSummaryReport.html' ).text
	}

}
