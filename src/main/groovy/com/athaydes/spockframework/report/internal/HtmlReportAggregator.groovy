package com.athaydes.spockframework.report.internal

import groovy.xml.MarkupBuilder

/**
 *
 * User: Renato
 */
@Singleton( lazy = true )
class HtmlReportAggregator extends AbstractHtmlCreator<Map> {

	final Map<String, Map> aggregatedData = [ : ]

	def stringFormatter = new StringFormatHelper()

	protected HtmlReportAggregator( ) {
		// provided for testing only (need to Mock it)
	}

	void aggregateReport( String specName, Map stats, String outputDir ) {
		this.outputDir = outputDir
		aggregatedData[ specName ] = stats
		def reportsDir = createReportsDir()
		if ( reportsDir.exists() ) {
			try {
				new File( reportsDir, 'index.html' )
						.write( reportFor( stats ) )
			} catch ( e ) {
				if(!silenceOutput) {
					e.printStackTrace()
					println "${this.class.name} failed to create aggregated report, Reason: $e"
				}
			}

		} else if(!silenceOutput) {
			println "${this.class.name} cannot create output directory: ${reportsDir.absolutePath}"
		}
	}

	@Override
	protected String reportHeader( Map data ) {
		'Specification run results'
	}

	@Override
	protected void writeSummary( MarkupBuilder builder, Map stats ) {
		def aggregateData = recomputeAggregateData()
		def cssClassIfTrue = { isTrue, String cssClass ->
			if ( isTrue ) [ 'class': cssClass ] else Collections.emptyMap()
		}
		builder.div( 'class': 'summary-report' ) {
			h3 'Specifications summary:'
			builder.div( 'class': 'date-test-ran', whenAndWho.whenAndWhoRanTest( stringFormatter ) )
			table( 'class': 'summary-table' ) {
				thead {
					th 'Total'
					th 'Passed'
					th 'Failed'
					th 'Feature failures'
					th 'Feature errors'
					th 'Success rate'
					th 'Total time'
				}
				tbody {
					tr {
						td aggregateData.total
						td aggregateData.passed
						td( cssClassIfTrue( aggregateData.failed, 'failure' ), aggregateData.failed )
						td( cssClassIfTrue( aggregateData.fFails, 'failure' ), aggregateData.fFails )
						td( cssClassIfTrue( aggregateData.fErrors, 'error' ), aggregateData.fErrors )
						td( cssClassIfTrue( aggregateData.failed, 'failure' ), stringFormatter
								.toPercentage( successRate( aggregateData.total, aggregateData.failed ) ) )
						td stringFormatter.toTimeDuration( aggregateData.time )
					}
				}
			}
		}
	}

	protected Map recomputeAggregateData( ) {
		def result = [ total: 0, passed: 0, failed: 0, fFails: 0, fErrors: 0, time: 0.0 ]
		aggregatedData.values().each { Map stats ->
			result.total += 1
			result.passed += ( stats.failures || stats.errors ? 0 : 1 )
			result.failed += ( stats.failures || stats.errors ? 1 : 0 )
			result.fFails += stats.failures
			result.fErrors += stats.errors
			result.time += stats.time
		}
		result
	}

	@Override
	protected void writeDetails( MarkupBuilder builder, Map ignored ) {
		builder.h3 'Specifications:'
		builder.table( 'class': 'summary-table' ) {
			thead {
				th 'Name'
				th 'Features'
				th 'Failed'
				th 'Errors'
				th 'Skipped'
				th 'Success rate'
				th 'Time'
			}
			tbody {
				aggregatedData.keySet().sort().each { String specName ->
					def stats = aggregatedData[ specName ]
					def cssClasses = [ ]
					if ( stats.failures ) cssClasses << 'failure'
					if ( stats.errors ) cssClasses << 'error'
					tr( cssClasses ? [ 'class': cssClasses.join( ' ' ) ] : null ) {
						td {
							a( href: "${specName}.html", specName )
						}
						td stats.totalRuns
						td stats.failures
						td stats.errors
						td stats.skipped
						td stringFormatter.toPercentage( stats.successRate )
						td stringFormatter.toTimeDuration( stats.time )
					}
				}
			}
		}

	}

}
