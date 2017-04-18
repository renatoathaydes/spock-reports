package com.athaydes.spockframework.report.internal

import com.athaydes.spockframework.report.IReportCreator
import com.athaydes.spockframework.report.util.Utils
import com.athaydes.spockframework.report.vivid.BlockCode
import com.athaydes.spockframework.report.vivid.SpecSourceCodeReader
import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder
import org.spockframework.runtime.model.BlockInfo
import org.spockframework.runtime.model.FeatureInfo
import org.spockframework.runtime.model.IterationInfo
import spock.lang.Ignore
import spock.lang.Issue
import spock.lang.See
import spock.lang.Title

import java.lang.annotation.Annotation

/**
 *
 * User: Renato
 */
@Slf4j
class HtmlReportCreator extends AbstractHtmlCreator<SpecData>
        implements IReportCreator {

    private static final defaultAggregator = new HtmlReportAggregator()

    final HtmlReportAggregator reportAggregator

    def stringFormatter = new StringFormatHelper()
    def problemWriter = new ProblemBlockWriter( stringFormatter: stringFormatter )
    def stringProcessor = new StringTemplateProcessor()
    private final SpecSourceCodeReader codeReader = new SpecSourceCodeReader()
    boolean showCodeBlocks = false
    boolean enabled = true

    HtmlReportCreator() {
        reportAggregator = defaultAggregator
    }

    HtmlReportCreator( HtmlReportAggregator reportAggregator ) {
        this.reportAggregator = reportAggregator
    }

    @Override
    void setTestSourceRoots( String roots ) {
        if ( roots ) {
            codeReader.testSourceRoots = roots
        }
    }

    void setFeatureReportCss( String css ) {
        super.setCss( css )
    }

    void setSummaryReportCss( String css ) {
        reportAggregator?.css = css
    }

    void setPrintThrowableStackTrace( String printStacktrace ) {
        problemWriter.printThrowableStackTrace = Boolean.parseBoolean( printStacktrace )
    }

    void setInlineCss( String inline ) {
        def doInline = Boolean.parseBoolean( inline )
        this.doInlineCss = doInline
        reportAggregator?.doInlineCss = doInline
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
        reportAggregator.projectName = projectName
    }

    @Override
    void setProjectVersion( String projectVersion ) {
        reportAggregator.projectVersion = projectVersion
    }

    @Override
    void setOutputDir( String out ) {
        this.outputDirectory = out
        reportAggregator?.outputDirectory = out
    }

    @Override
    String cssDefaultName() { 'feature-report.css' }

    void done() {
        if ( enabled ) {
            reportAggregator?.writeOut()
        }
    }

    @Override
    void createReportFor( SpecData data ) {
        if ( !enabled ) {
            return
        }

        def specClassName = Utils.getSpecClassName( data )
        def reportsDir = outputDirectory ? Utils.createDir( outputDirectory ) : null
        if ( reportsDir?.isDirectory() ) {
            try {
                if ( showCodeBlocks ) {
                    codeReader.read( data )
                }
                new File( reportsDir, specClassName + '.html' )
                        .write( reportFor( data ) )
            } catch ( e ) {
                log.warn( "Failed to create report for $specClassName", e )
            }

        } else {
            log.warn "Cannot create output directory: {}", reportsDir?.absolutePath
        }
    }

    @Override
    protected String reportHeader( SpecData data ) {
        "Report for ${Utils.getSpecClassName( data )}"
    }

    void writeSummary( MarkupBuilder builder, SpecData data ) {
        builder.div( 'class': 'summary-report' ) {
            h3 'Summary:'
            builder.div( 'class': 'date-test-ran', whenAndWho.whenAndWhoRanTest( stringFormatter ) )
            table( 'class': 'summary-table' ) {
                thead {
                    th 'Executed features'
                    th 'Failures'
                    th 'Errors'
                    th 'Skipped'
                    th 'Success rate'
                    th 'Time'
                }
                tbody {
                    tr {
                        def stats = Utils.stats( data )
                        td stats.totalRuns
                        td stats.failures
                        td stats.errors
                        td stats.skipped
                        td stringFormatter.toPercentage( stats.successRate )
                        td stringFormatter.toTimeDuration( stats.time )
                        reportAggregator?.aggregateReport( data, stats )
                    }
                }
            }
        }
    }

    protected void writeDetails( MarkupBuilder builder, SpecData data ) {
        def specTitle = Utils.specAnnotation( data, Title )?.value() ?: ''
        if ( specTitle ) {
            builder.pre( 'class': 'title', specTitle )
        }
        def narrative = data.info.narrative ?: ''
        if ( narrative ) {
            builder.pre( 'class': 'narrative', narrative )
        }
        def issues = Utils.specAnnotation( data, Issue )
        if ( issues ) {
            writeIssuesOrSees( builder, issues, 'Issues:' )
        }
        def sees = Utils.specAnnotation( data, See )
        if ( sees ) {
            writeIssuesOrSees( builder, sees, 'See:' )
        }
        builder.h3 "Features:"
        builder.table( 'class': 'features-table' ) {
            colgroup {
                col( 'class': 'block-kind-col' )
                col( 'class': 'block-text-col' )
            }
            tbody {
                writeFeature( builder, data )
            }
        }
    }

    private void writeFeatureToc( MarkupBuilder builder, SpecData data ) {
        builder.ul( id: 'toc' ) {
            for ( FeatureInfo feature in data.info.allFeaturesInExecutionOrder ) {
                FeatureRun run = data.featureRuns.find { it.feature == feature }
                if ( run && Utils.isUnrolled( feature ) ) {
                    run.failuresByIteration.eachWithIndex { iteration, problems, int index ->
                        final String name = Utils.featureNameFrom( feature, iteration, index )
                        final cssClass = problems.any( Utils.&isError ) ? 'error' :
                                problems.any( Utils.&isFailure ) ? 'failure' :
                                        feature.skipped ? 'ignored' : 'pass'
                        li {
                            a( href: "#${name.hashCode()}", 'class': "feature-toc-$cssClass", name )
                        }
                    }
                } else {
                    final failures = run ? Utils.countProblems( [ run ], Utils.&isFailure ) : 0
                    final errors = run ? Utils.countProblems( [ run ], Utils.&isError ) : 0
                    final cssClass = errors ? 'error' : failures ? 'failure' : !run ? 'ignored' : 'pass'
                    li {
                        a( href: "#${feature.name.hashCode()}", 'class': "feature-toc-$cssClass", feature.name )
                    }
                }
            }
        }
    }

    private void writeFeature( MarkupBuilder builder, SpecData data ) {
        if ( data.initializationError ) {
            problemWriter.problemsContainer( builder ) {
                problemWriter.writeProblemMsgs( builder, [ data.initializationError.exception ] )
            }
            return
        }

        if ( excludeToc.toLowerCase() != 'true' ) writeFeatureToc( builder, data )
        for ( FeatureInfo feature in data.info.allFeaturesInExecutionOrder ) {
            FeatureRun run = data.featureRuns.find { it.feature == feature }
            if ( run && Utils.isUnrolled( feature ) ) {
                run.failuresByIteration.eachWithIndex { iteration, problems, int index ->
                    String name = Utils.featureNameFrom( feature, iteration, index )
                    final cssClass = problems.any( Utils.&isError ) ? 'error' :
                            problems.any( Utils.&isFailure ) ? 'failure' :
                                    feature.skipped ? 'ignored' : ''
                    writeFeatureDescription( builder, name, cssClass,
                            feature.description.getAnnotation( Ignore ),
                            feature.description.getAnnotation( Issue ),
                            feature.description.getAnnotation( See ) )
                    writeFeatureBlocks( builder, feature, iteration )
                    problemWriter.writeProblemBlockForIteration( builder, iteration, problems )
                }
            } else {
                final failures = run ? Utils.countProblems( [ run ], Utils.&isFailure ) : 0
                final errors = run ? Utils.countProblems( [ run ], Utils.&isError ) : 0
                final cssClass = errors ? 'error' : failures ? 'failure' : !run ? 'ignored' : ''
                writeFeatureDescription( builder, feature.name, cssClass,
                        feature.description.getAnnotation( Ignore ),
                        feature.description.getAnnotation( Issue ),
                        feature.description.getAnnotation( See ) )
                writeFeatureBlocks( builder, feature )
                if ( run ) {
                    writeRun( builder, run )
                    problemWriter.writeProblemBlockForAllIterations( builder, run, errors > 0, failures > 0 )
                }
            }
        }
    }


    private void writeFeatureBlocks( MarkupBuilder builder, FeatureInfo feature, IterationInfo iteration = null ) {
        if ( showCodeBlocks ) {
            writeBlocksFromCode( builder, feature, iteration )
        } else {
            writeBlocks( builder, feature, iteration )
        }
    }

    private void writeBlocks( MarkupBuilder builder, FeatureInfo feature, IterationInfo iteration ) {
        for ( BlockInfo block in feature.blocks ) {
            if ( !Utils.isEmptyOrContainsOnlyEmptyStrings( block.texts ) )
                block.texts.eachWithIndex { blockText, index ->
                    if ( iteration ) {
                        blockText = stringProcessor.process( blockText, feature.dataVariables, iteration )
                    }
                    writeBlockRow( builder, trCssClass( feature ),
                            ( index == 0 ? block.kind : 'and' ), blockText )
                }
            else if ( !hideEmptyBlocks ) {
                writeBlockRow( builder, trCssClass( feature ), block.kind, '----' )
            }
        }
    }

    private void writeBlocksFromCode( MarkupBuilder builder, FeatureInfo feature, IterationInfo iteration ) {
        def blocks = codeReader.getBlocks( feature )
        for ( BlockCode block in blocks ) {
            def text = iteration && block.text ?
                    stringProcessor.process( block.text, feature.dataVariables, iteration ) :
                    ( block.text ?: '' )
            def blockKind = block.label ?: 'Block:'
            writeBlockRowsFromCode( builder, trCssClass( feature ), blockKind, block.statements, text )
        }
    }

    private trCssClass( FeatureInfo feature ) {
        feature.skipped ? [ 'class': 'ignored' ] : null
    }

    private writeBlockRow( MarkupBuilder builder, cssClass, blockKind, text ) {
        builder.tr( cssClass ) {
            writeBlockKindTd( builder, blockKind )
            writeBlockTextTd( builder, text )
        }
    }

    private writeBlockRowsFromCode( MarkupBuilder builder, cssClass, blockKind, List statements, text ) {
        if ( text ) {
            writeBlockRow( builder, cssClass, blockKind, text )
            if ( statements ) builder.tr {
                td()
                writeCodeTd( builder, statements )
            }
        } else if ( statements ) builder.tr( cssClass ) {
            writeBlockKindTd( builder, blockKind )
            writeCodeTd( builder, statements )
        }
    }

    private void writeBlockKindTd( MarkupBuilder builder, blockKind ) {
        builder.td {
            div( 'class': 'block-kind', Utils.block2String[ blockKind ] )
        }
    }

    private void writeBlockTextTd( MarkupBuilder builder, text ) {
        builder.td {
            div( 'class': 'block-text', text )
        }
    }

    private writeCodeTd( MarkupBuilder builder, List statements ) {
        builder.td {
            pre( 'class': 'block-source', statements.join( '\n' ) )
        }
    }

    private void writeRun( MarkupBuilder builder, FeatureRun run ) {
        if ( !run.feature.parameterized ) return
        builder.tr {
            writeBlockKindTd( builder, 'examples' )
            td {
                div( 'class': 'spec-examples' ) {
                    table( 'class': 'ex-table' ) {
                        thead {
                            for ( param in run.feature.parameterNames ) {
                                th( 'class': 'ex-header', param )
                            }
                        }
                        tbody {
                            run.failuresByIteration.each { iteration, errors ->
                                writeIteration( builder, iteration, errors )
                            }
                        }
                    }
                }
            }
            td {
                div( 'class': 'spec-status', Utils.iterationsResult( run ) )
            }
        }

    }

    private void writeIteration( MarkupBuilder builder, IterationInfo iteration,
                                 List<SpecProblem> errors ) {
        builder.tr( 'class': errors ? 'ex-fail' : 'ex-pass' ) {
            for ( value in iteration.dataValues ) {
                def writableValue = value instanceof Runnable ?
                        "<executable>" :
                        ( value == null ? '<null>' : value.toString() )
                td( 'class': 'ex-value', writableValue )
            }
            td( 'class': 'ex-result', iterationResult( errors ) )
        }
    }

    private static String iterationResult( List<SpecProblem> errors ) {
        errors ? 'FAIL' : 'OK'
    }

    private void writeFeatureDescription( MarkupBuilder builder, String name,
                                          String cssClass,
                                          Ignore ignoreAnnotation,
                                          Issue issueAnnotation,
                                          See seeAnnotation ) {
        def ignoreReason = ''
        if ( cssClass == 'ignored' && ignoreAnnotation ) {
            ignoreReason = ignoreAnnotation.value()
        }

        cssClass = cssClass ? ' ' + cssClass : ''

        builder.tr {
            td( colspan: '10' ) {
                div( 'class': 'feature-description' + cssClass, id: name.hashCode() ) {
                    span name
                    writeLinkBackToTop builder
                    if ( ignoreReason ) {
                        div()
                        span( 'class': 'reason', ignoreReason )
                    }
                    writeIssuesOrSees builder, issueAnnotation, 'Issues:'
                    writeIssuesOrSees builder, seeAnnotation, 'See:'
                }
            }
        }
    }

    private void writeIssuesOrSees( MarkupBuilder builder, Annotation annotation, String description ) {
        if ( annotation?.value() ) {
            builder.div( 'class': 'issues' ) {
                div( description )
                ul {
                    for ( String value in annotation.value() ) {
                        li {
                            if ( Utils.isUrl( value ) ) {
                                a( 'href': value ) {
                                    mkp.yield value
                                }
                            } else {
                                span stringFormatter.escapeXml( value )
                            }
                        }
                    }
                }
            }
        }
    }

    private void writeLinkBackToTop( MarkupBuilder builder ) {
        builder.span( style: 'float: right; font-size: 60%;' ) {
            a( href: '#toc', 'Return' )
        }
    }

}
