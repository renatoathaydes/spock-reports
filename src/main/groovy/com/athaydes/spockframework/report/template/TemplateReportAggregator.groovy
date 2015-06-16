package com.athaydes.spockframework.report.template

import com.athaydes.spockframework.report.internal.SpecData
import com.athaydes.spockframework.report.util.Utils
import groovy.text.GStringTemplateEngine
import groovy.util.logging.Log

@Singleton( lazy = true )
@Log
class TemplateReportAggregator {

    private final Map<String, Map> aggregatedData = [ : ]

    void addData( SpecData specData ) {
        aggregatedData[ specData.info.description.className ] = Utils.stats( specData )
    }

    private String summaryWith( String templateLocation ) {
        def template = this.class.getResource( templateLocation )
        assert template, "Summary template location does not exist: $templateLocation"

        def engine = new GStringTemplateEngine()

        engine.createTemplate( template )
                .make( [ data: aggregatedData ] )
                .toString()
    }

    void writeOut( File summaryFile, String templateLocation ) {
        try {
            summaryFile.write( summaryWith( templateLocation ) )
        } catch ( e ) {
            log.warning( "Problem writing summary report: $e" )
        }
    }

}
