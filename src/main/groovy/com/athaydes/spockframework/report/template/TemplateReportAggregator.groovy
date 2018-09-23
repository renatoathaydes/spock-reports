package com.athaydes.spockframework.report.template

import com.athaydes.spockframework.report.internal.SpecData
import com.athaydes.spockframework.report.internal.StringFormatHelper
import com.athaydes.spockframework.report.util.Utils
import groovy.text.GStringTemplateEngine
import groovy.util.logging.Slf4j
import spock.lang.Title

import static com.athaydes.spockframework.report.internal.ReportDataAggregator.getAllAggregatedDataAndPersistLocalData

@Slf4j
class TemplateReportAggregator {

    private final Map<String, Map> aggregatedData = [ : ]

    volatile String projectName
    volatile String projectVersion
    volatile String aggregatedJsonReportDir

    void addData( SpecData data ) {
        def specName = Utils.getSpecClassName( data )
        log.debug( "Adding data to report {}", specName )

        def stats = Utils.stats( data )
        def allFeatures = data.info.allFeaturesInExecutionOrder.groupBy { feature -> Utils.isSkipped( feature ) }
        def specTitle = Utils.specAnnotation( data, Title )?.value() ?: ''
        def narrative = data.info.narrative ?: ''

        aggregatedData[ specName ] = Utils.createAggregatedData(
                allFeatures[ false ], allFeatures[ true ], stats, specTitle, narrative )
    }

    private String summary( String templateLocation, Map allData ) {
        def template = this.class.getResource( templateLocation )
        if ( !template ) {
            log.warn( "Summary template location does not exist: $templateLocation" )
            throw new RuntimeException( 'SpockReports: TemplateReportAggregator extension could not create ' +
                    'report as template could not be found at ' + templateLocation )
        }

        def engine = new GStringTemplateEngine()

        engine.createTemplate( template )
                .make( [ data          : allData,
                         'utils'       : Utils,
                         'fmt'         : new StringFormatHelper(),
                         projectName   : projectName,
                         projectVersion: projectVersion ] )
                .toString()
    }

    void writeOut( File summaryFile, String templateLocation ) {
        final reportsDir = summaryFile.parentFile
        final jsonDir = aggregatedJsonReportDir ? new File( aggregatedJsonReportDir ) : reportsDir
        log.info( "Writing summary report to {}", summaryFile.absolutePath )

        try {
            def allData = getAllAggregatedDataAndPersistLocalData( jsonDir, aggregatedData )
            aggregatedData.clear()
            summaryFile.write summary( templateLocation, allData )
        } catch ( e ) {
            log.warn( "${this.class.name} failed to create aggregated report", e )
        }
    }

}
