package com.athaydes.spockframework.report.internal

import com.athaydes.spockframework.report.util.Utils
import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder

import static com.athaydes.spockframework.report.internal.ReportDataAggregator.getAllAggregatedDataAndPersistLocalData

/**
 *
 * User: Renato
 */
@Singleton( lazy = true )
@Slf4j
class HtmlReportAggregator extends AbstractHtmlCreator<Map> {

    final Map<String, Map> aggregatedData = [ : ]

    def stringFormatter = new StringFormatHelper()

    protected HtmlReportAggregator() {
        // provided for testing only (need to Mock it)
    }

    @Override
    String cssDefaultName() { 'summary-report.css' }

    void aggregateReport( String specName, Map stats ) {
        aggregatedData[ specName ] = stats
    }

    void writeOut() {
        final reportsDir = outputDirectory as File // try to force it into being a File!
        if ( existsOrCanCreate( reportsDir ) ) {
            final aggregatedReport = new File( reportsDir, 'index.html' )

            try {
                def allData = getAllAggregatedDataAndPersistLocalData( reportsDir, aggregatedData )
                aggregatedData.clear()
                aggregatedReport.write( reportFor( allData ) )
            } catch ( e ) {
                log.warn( "Failed to create aggregated report", e )
            }
        } else {
            log.warn "Cannot create output directory: {}", reportsDir?.absolutePath
        }
    }

    def boolean existsOrCanCreate( File reportsDir ) {
        reportsDir?.exists() || reportsDir?.mkdirs()
    }

    @Override
    protected String reportHeader( Map data ) {
        'Specification run results'
    }

    @Override
    protected void writeSummary( MarkupBuilder builder, Map stats ) {
        def aggregateData = Utils.aggregateStats( stats )
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
    protected void writeDetails( MarkupBuilder builder, Map data ) {
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
                data.keySet().sort().each { String specName ->
                    def stats = data[ specName ]
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
