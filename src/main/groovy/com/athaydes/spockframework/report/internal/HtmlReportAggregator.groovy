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

    void aggregateReport( SpecData data, Map stats ) {
        def specName = data.info.description.className
        def allFeatures = data.info.allFeatures.groupBy { feature -> feature.skipped }

        aggregatedData[ specName ] = Utils.createAggregatedData(
                allFeatures[ false ], allFeatures[ true ], stats )
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
    protected void writeSummary( MarkupBuilder builder, Map json ) {
        def stats = Utils.aggregateStats( json )
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
                        td stats.total
                        td stats.passed
                        td( cssClassIfTrue( stats.failed, 'failure' ), stats.failed )
                        td( cssClassIfTrue( stats.fFails, 'failure' ), stats.fFails )
                        td( cssClassIfTrue( stats.fErrors, 'error' ), stats.fErrors )
                        td( cssClassIfTrue( stats.failed, 'failure' ), stringFormatter
                                .toPercentage( Utils.successRate( stats.total, stats.failed ) ) )
                        td stringFormatter.toTimeDuration( stats.time )
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
                    def stats = data[ specName ].stats
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
