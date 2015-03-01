package com.athaydes.spockframework.report.template

import com.athaydes.spockframework.report.IReportCreator
import com.athaydes.spockframework.report.internal.FeatureRun
import com.athaydes.spockframework.report.internal.SpecData
import com.athaydes.spockframework.report.util.Utils
import groovy.text.GStringTemplateEngine
import groovy.util.logging.Log
import org.spockframework.runtime.model.FeatureInfo

/**
 * IReportCreator which uses a user-provided template to generate spock-reports.
 */
@Log
class TemplateReportCreator implements IReportCreator {

    // IReportCreator shared properties
    String outputDir
    boolean hideEmptyBlocks

    // TemplateReportCreator properties
    String templateFile
    String reportFileExtension

    @Override
    void createReportFor( SpecData data ) {
        def specClassName = data.info.description.className
        def reportsDir = Utils.createDir( outputDir )
        def reportFile = new File( reportsDir, specClassName + '.' + reportFileExtension )
        reportFile.delete()
        try {
            if ( reportsDir.isDirectory() ) {
                reportFile.write( reportFor( data ) )
            } else {
                println "${this.class.name} cannot create output directory: ${reportsDir.absolutePath}"
            }
        } catch ( e ) {
            println "Unexpected error creating report: $e"
        }
    }

    String reportFor( SpecData data ) {
        def templateFileUrl = this.class.getResource( templateFile )
        if ( !templateFileUrl ) {
            throw new RuntimeException( "Template File does not exist: $templateFile" )
        }

        def engine = new GStringTemplateEngine()

        def unrolledFeatures = createUnrolledFeaturesCallback( data )
        def regularFeatures = createRegularFeaturesCallback( data )

        def template = engine.createTemplate( templateFileUrl )
                .make( [ data            : data,
                         hideEmptyBlocks : hideEmptyBlocks,
                         unrolledFeatures: unrolledFeatures,
                         regularFeatures : regularFeatures ] )
        template.toString()
    }

    def createUnrolledFeaturesCallback( SpecData data ) {
        return { callback ->
            data.info.allFeatures.each { FeatureInfo feature ->
                FeatureRun run = data.featureRuns.find { it.feature == feature }
                if ( run && Utils.isUnrolled( feature ) ) {
                    run.failuresByIteration.each { iteration, problems ->
                        final name = feature.iterationNameProvider.getName( iteration )
                        final result = problems.any( Utils.&isError ) ? 'ERROR' :
                                problems.any( Utils.&isFailure ) ? 'FAILURE' :
                                        feature.skipped ? 'IGNORED' : 'PASS'
                        callback.call( feature, name, result, iteration, problems )
                    }
                }
            }
        }
    }

    def createRegularFeaturesCallback( SpecData data ) {
        return { callback ->
            data.info.allFeatures.each { FeatureInfo feature ->
                FeatureRun run = data.featureRuns.find { it.feature == feature }
                if ( !run || !Utils.isUnrolled( feature ) ) {
                    final failures = run ? Utils.countProblems( [ run ], Utils.&isFailure ) : 0
                    final errors = run ? Utils.countProblems( [ run ], Utils.&isError ) : 0
                    final result = errors ? 'ERROR' : failures ? 'FAIL' : !run ? 'IGNORED' : 'PASS'
                    callback.call( feature, feature.name, result, run )
                }
            }
        }
    }

}
