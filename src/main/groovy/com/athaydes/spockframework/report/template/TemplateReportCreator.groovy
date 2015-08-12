package com.athaydes.spockframework.report.template

import com.athaydes.spockframework.report.IReportCreator
import com.athaydes.spockframework.report.internal.FeatureRun
import com.athaydes.spockframework.report.internal.SpecData
import com.athaydes.spockframework.report.internal.StringTemplateProcessor
import com.athaydes.spockframework.report.util.Utils
import groovy.text.GStringTemplateEngine
import groovy.util.logging.Log
import org.spockframework.runtime.model.BlockInfo
import org.spockframework.runtime.model.FeatureInfo
import org.spockframework.runtime.model.IterationInfo

/**
 * IReportCreator which uses a user-provided template to generate spock-reports.
 */
@Log
class TemplateReportCreator implements IReportCreator {

    final stringProcessor = new StringTemplateProcessor()

    // IReportCreator shared properties
    String outputDir
    boolean hideEmptyBlocks

    // TemplateReportCreator properties
    String specTemplateFile
    String reportFileExtension
    String summaryTemplateFile
    String summaryFileName

    void done() {
        def reportsDir = Utils.createDir( outputDir )
        TemplateReportAggregator.instance
                .writeOut( new File( reportsDir, summaryFileName ), summaryTemplateFile )
    }

    @Override
    void createReportFor( SpecData data ) {
        def specClassName = data.info.description.className
        def reportsDir = Utils.createDir( outputDir )
        def reportFile = new File( reportsDir, specClassName + '.' + reportFileExtension )
        reportFile.delete()
        try {
            if ( reportsDir.isDirectory() ) {
                reportFile.write( reportFor( data ) )
                TemplateReportAggregator.instance.addData( data )
            } else {
                log.warning "${this.class.name} cannot create output directory: ${reportsDir.absolutePath}"
            }
        } catch ( e ) {
            log.warning "Unexpected error creating report: $e"
        }
    }

    String reportFor( SpecData data ) {
        def templateFileUrl = this.class.getResource( specTemplateFile )
        if ( !templateFileUrl ) {
            throw new RuntimeException( "Template File does not exist: $specTemplateFile" )
        }

        def engine = new GStringTemplateEngine()

        def featuresCallback = createFeaturesCallback data

        engine.createTemplate( templateFileUrl )
                .make( [ reportCreator: this,
                         data         : data,
                         features     : featuresCallback ] )
                .toString()
    }

    def createFeaturesCallback( SpecData data ) {
        return [ forEach: { Closure callback ->
            for ( feature in data.info.allFeatures ) {
                FeatureRun run = data.featureRuns.find { it.feature == feature }
                if ( run && Utils.isUnrolled( feature ) ) {
                    handleUnrolledFeature( run, feature, callback )
                } else {
                    handleRegularFeature( run, callback, feature )
                }
            }
        } ]
    }

    protected void handleRegularFeature( FeatureRun run, Closure callback, FeatureInfo feature ) {
        final failures = run ? Utils.countProblems( [ run ], Utils.&isFailure ) : 0
        final errors = run ? Utils.countProblems( [ run ], Utils.&isError ) : 0
        final result = errors ? 'ERROR' : failures ? 'FAIL' : !run ? 'IGNORED' : 'PASS'
        final problemsByIteration = run ? Utils.problemsByIteration( run.failuresByIteration ) : [ : ]
        callback.call( feature.name, result, processedBlocks( feature ), problemsByIteration, feature.parameterNames )
    }

    protected void handleUnrolledFeature( FeatureRun run, FeatureInfo feature, Closure callback ) {
        run.failuresByIteration.each { iteration, problems ->
            final name = feature.iterationNameProvider.getName( iteration )
            final result = problems.any( Utils.&isError ) ? 'ERROR' :
                    problems.any( Utils.&isFailure ) ? 'FAILURE' :
                            feature.skipped ? 'IGNORED' : 'PASS'
            final problemsByIteration = Utils.problemsByIteration( [ ( iteration ): problems ] )
            callback.call( name, result, processedBlocks( feature, iteration ), problemsByIteration, feature.parameterNames )
        }
    }

    protected List processedBlocks( FeatureInfo feature, IterationInfo iteration = null ) {
        feature.blocks.collect { BlockInfo block ->
            if ( !Utils.isEmptyOrContainsOnlyEmptyStrings( block.texts ) ) {
                int index = 0
                block.texts.collect { blockText ->
                    if ( iteration ) {
                        blockText = stringProcessor.process( blockText, feature.dataVariables, iteration )
                    }
                    [ kind: Utils.block2String[ ( index++ ) == 0 ? block.kind : 'AND' ], text: blockText ]
                }
            } else if ( !hideEmptyBlocks )
                [ kind: Utils.block2String[ block.kind ], text: '----' ]
            else
                [ : ]
        }.findAll { !it.empty }.flatten()
    }

}
