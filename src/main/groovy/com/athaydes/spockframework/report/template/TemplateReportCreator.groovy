package com.athaydes.spockframework.report.template

import com.athaydes.spockframework.report.IReportCreator
import com.athaydes.spockframework.report.internal.FeatureRun
import com.athaydes.spockframework.report.internal.SpecData
import com.athaydes.spockframework.report.internal.StringFormatHelper
import com.athaydes.spockframework.report.internal.StringTemplateProcessor
import com.athaydes.spockframework.report.util.Utils
import com.athaydes.spockframework.report.vivid.BlockCode
import com.athaydes.spockframework.report.vivid.SpecSourceCodeReader
import groovy.text.GStringTemplateEngine
import groovy.util.logging.Slf4j
import org.spockframework.runtime.model.BlockInfo
import org.spockframework.runtime.model.FeatureInfo
import org.spockframework.runtime.model.IterationInfo

import static java.util.Collections.emptyList
import static java.util.Collections.emptyMap

/**
 * IReportCreator which uses a user-provided template to generate spock-reports.
 */
@Slf4j
class TemplateReportCreator implements IReportCreator {

    static final reportAggregator = new TemplateReportAggregator()

    final stringProcessor = new StringTemplateProcessor()

    // IReportCreator shared properties
    String outputDir
    boolean hideEmptyBlocks
    boolean showCodeBlocks

    // TemplateReportCreator properties
    String specTemplateFile
    String reportFileExtension
    String summaryTemplateFile
    String summaryFileName
    boolean enabled = true

    private final SpecSourceCodeReader codeReader = new SpecSourceCodeReader()

    @Override
    void setTestSourceRoots( String roots ) {
        if ( roots ) {
            codeReader.testSourceRoots = roots
        }
    }

    void setEnabled( String enabled ) {
        try {
            this.@enabled = Boolean.parseBoolean( enabled )
        } catch ( e ) {
            log.warn( "Problem parsing 'enabled' property, invalid value: $enabled", e )
        }
    }

    @Override
    void setProjectName( String projectName ) {
        reportAggregator?.projectName = projectName
    }

    @Override
    void setProjectVersion( String projectVersion ) {
        reportAggregator?.projectVersion = projectVersion
    }

    @Override
    void setAggregatedJsonReportDir( String path ) {
        reportAggregator?.aggregatedJsonReportDir = path
    }

    void done() {
        if ( !enabled ) {
            return
        }

        def reportsDir = Utils.createDir( outputDir )

        reportAggregator?.writeOut(
                new File( reportsDir, summaryFileName ),
                summaryTemplateFile )
    }

    @Override
    void createReportFor( SpecData data ) {
        if ( !enabled ) {
            return
        }

        def specClassName = Utils.getSpecClassName( data )
        def reportsDir = Utils.createDir( outputDir )
        def reportFile = new File( reportsDir, specClassName + '.' + reportFileExtension )
        reportFile.delete()
        try {
            if ( reportsDir.isDirectory() ) {
                log.debug( "Writing report to file: {}", reportFile )
                reportFile.write( reportFor( data ), 'UTF-8' )
                reportAggregator.addData( data )
            } else {
                log.warn "${this.class.name} cannot create output directory: ${reportsDir.absolutePath}"
            }
        } catch ( e ) {
            log.warn "Unexpected error creating report", e
        }
    }

    String reportFor( SpecData data ) {
        def templateFileUrl = this.class.getResource( specTemplateFile )
        if ( !templateFileUrl ) {
            throw new RuntimeException( "Template File does not exist: $specTemplateFile" )
        }

        def engine = new GStringTemplateEngine()

        def featuresCallback = createFeaturesCallback data

        if ( showCodeBlocks ) {
            codeReader.read( data )
        }

        engine.createTemplate( templateFileUrl )
                .make( [ reportCreator: this,
                         'utils'      : Utils,
                         'fmt'        : new StringFormatHelper(),
                         data         : data,
                         features     : featuresCallback ] )
                .toString()
    }

    def createFeaturesCallback( SpecData data ) {
        return [ eachFeature: { Closure callback ->
            for ( feature in data.info.allFeaturesInExecutionOrder ) {
                callback.delegate = feature
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
        final isSkipped = !run || Utils.isSkipped( feature )
        final result = errors ? 'ERROR' : failures ? 'FAIL' : isSkipped ? 'IGNORED' : 'PASS'
        final problemsByIteration = run ? Utils.iterationData( run.failuresByIteration, run.timeByIteration ) : [ : ]
        callback.call( feature.name, result, processedBlocks( feature ), problemsByIteration, feature.parameterNames )
    }

    protected void handleUnrolledFeature( FeatureRun run, FeatureInfo feature, Closure callback ) {
        run.failuresByIteration.eachWithIndex { iteration, problems, index ->
            final name = Utils.featureNameFrom( feature, iteration, index )
            final result = problems.any( Utils.&isError ) ? 'ERROR' :
                    problems.any( Utils.&isFailure ) ? 'FAILURE' :
                            Utils.isSkipped( feature ) ? 'IGNORED' : 'PASS'
            final time = run.timeByIteration.get( iteration, 0L )
            final problemsByIteration = Utils.iterationData( [ ( iteration ): problems ], [ (iteration ) : time ] )
            callback.call( name, result, processedBlocks( feature, iteration ), problemsByIteration, feature.parameterNames )
        }
    }

    protected List processedBlocks( FeatureInfo feature, IterationInfo iteration = null ) {
        if ( showCodeBlocks ) {
            def result = processedBlocksFromCode( feature, iteration )
            if ( result ) { // only return if we found something, otherwise, run the conventional procedure
                return result
            }
        }

        // as we don't have the AST, we need to use the old way to get the block text from Spock's API
        feature.blocks.collect { BlockInfo block ->
            List<String> blockTexts = block.texts

            if ( !Utils.isEmptyOrContainsOnlyEmptyStrings( blockTexts ) ) {
                int index = 0
                blockTexts.collect { blockText ->
                    if ( iteration ) {
                        blockText = stringProcessor.process( blockText, feature.dataVariables, iteration )
                    }
                    [ kind      : Utils.block2String[ ( index++ ) == 0 ? block.kind : 'and' ],
                      text      : blockText,
                      sourceCode: emptyList() ]
                }
            } else if ( !hideEmptyBlocks ) {
                [ [ kind      : Utils.block2String[ block.kind ],
                    text      : '----',
                    sourceCode: emptyList() ] ]
            } else {
                [ [ : ] ]
            }
        }.flatten().findAll { Map item -> !item.isEmpty() }
    }

    private List processedBlocksFromCode( FeatureInfo feature, IterationInfo iteration ) {
        def blocks = codeReader.getBlocks( feature )

        blocks.collect { BlockCode block ->
            def blockText = iteration && block.text ?
                    stringProcessor.process( block.text, feature.dataVariables, iteration ) :
                    ( block.text ?: '' )
            def blockKind = Utils.block2String[ block.label ] ?: 'Block:' // use an emergency label if something goes wrong

            if ( blockText || block.statements || !hideEmptyBlocks ) {
                [ kind: blockKind, text: blockText, sourceCode: block.statements ]
            } else {
                emptyMap()
            }
        }.findAll { Map map -> !map.isEmpty() }
    }

}
