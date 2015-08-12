package com.athaydes.spockframework.report.template

import com.athaydes.spockframework.report.internal.SpecData
import com.athaydes.spockframework.report.util.Utils
import groovy.text.GStringTemplateEngine
import groovy.util.logging.Log

import java.util.logging.Level

import static com.athaydes.spockframework.report.internal.ReportDataAggregator.getAllAggregatedDataAndPersistLocalData

@Singleton( lazy = true )
@Log
class TemplateReportAggregator {

    private final Map<String, Map> aggregatedData = [ : ]

    void addData( SpecData specData ) {
        aggregatedData[ specData.info.description.className ] = Utils.stats( specData )
    }

    private String summary( String templateLocation, Map allData ) {
        def template = this.class.getResource( templateLocation )
        assert template, "Summary template location does not exist: $templateLocation"

        def engine = new GStringTemplateEngine()

        engine.createTemplate( template )
                .make( [ data: allData ] )
                .toString()
    }

    void writeOut( File summaryFile, String templateLocation ) {
        final reportsDir = summaryFile.parentFile

        try {
            def allData = getAllAggregatedDataAndPersistLocalData( reportsDir, aggregatedData )
            aggregatedData.clear()
            summaryFile.write summary( templateLocation, allData )
        } catch ( e ) {
            log.log( Level.WARNING, "${this.class.name} failed to create aggregated report", e )
        }
    }

}
