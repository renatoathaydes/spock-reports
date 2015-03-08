package com.athaydes.spockframework.report.internal

import com.athaydes.spockframework.report.util.Utils
import groovy.util.logging.Log
import groovy.xml.MarkupBuilder

import java.util.logging.Level

/**
 *
 * User: Renato
 */
@Singleton( lazy = true )
@Log
class HtmlReportAggregator extends AbstractHtmlCreator<Map> {

	final Map<String, Map> aggregatedData = [ : ]

	def stringFormatter = new StringFormatHelper()

	protected HtmlReportAggregator( ) {
		// provided for testing only (need to Mock it)
	}

	void aggregateReport( String specName, Map stats, String outputDir ) {
		this.outputDir = outputDir
		aggregatedData[ specName ] = stats
        def reportsDir = Utils.createDir( outputDir )
		if ( reportsDir.exists() ) {
			try {
				new File( reportsDir, 'index.html' )
						.write( reportFor( stats ) )
			} catch ( e ) {
				log.log(Level.FINE, "${this.class.name} failed to create aggregated report", e)
			}

		} else {
			log.fine "${this.class.name} cannot create output directory: ${reportsDir.absolutePath}"
		}
	}

	@Override
	protected String reportHeader( Map data ) {
		'Specification run results'
	}

	@Override
	protected void writeSummary( MarkupBuilder builder, Map stats ) {
		def aggregateData = Utils.aggregateStats( this.aggregatedData )
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
								.toPercentage( Utils.successRate( aggregateData.total, aggregateData.failed ) ) )
						td stringFormatter.toTimeDuration( aggregateData.time )
					}
				}
			}
		}
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
