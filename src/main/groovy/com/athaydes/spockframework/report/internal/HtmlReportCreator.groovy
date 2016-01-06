package com.athaydes.spockframework.report.internal

import com.athaydes.spockframework.report.IReportCreator
import com.athaydes.spockframework.report.util.Utils
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

    def reportAggregator = HtmlReportAggregator.instance
    def stringFormatter = new StringFormatHelper()
    def problemWriter = new ProblemBlockWriter( stringFormatter: stringFormatter )
    def stringProcessor = new StringTemplateProcessor()

    void setFeatureReportCss( String css ) {
        super.setCss( css )
    }

    void setSummaryReportCss( String css ) {
        reportAggregator?.css = css
    }

    void setPrintThrowableStackTrace( boolean printStacktrace ) {
        problemWriter.printThrowableStackTrace = printStacktrace
    }

    void done() {
        reportAggregator?.writeOut( outputDir )
    }

    @Override
    void createReportFor( SpecData data ) {
        def specClassName = data.info.description.className
        def reportsDir = outputDir ? Utils.createDir( outputDir ) : null
        if ( reportsDir?.isDirectory() ) {
            try {
                new File( reportsDir, specClassName + '.html' )
                        .write( reportFor( data ) )
            } catch ( e ) {
                log.warn( "${this.class.name} failed to create report for $specClassName", e )
            }

        } else {
            log.warn "${this.class.name} cannot create output directory: ${reportsDir?.absolutePath}"
        }
    }

    @Override
    protected String reportHeader( SpecData data ) {
        "Report for ${data.info.description.className}"
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
                        reportAggregator?.aggregateReport( data.info.description.className, stats )
                    }
                }
            }
        }
    }

    protected void writeDetails( MarkupBuilder builder, SpecData data ) {
        def specTitle = Utils.specAnnotation( data, Title )?.value() ?: ''
        def narrative = ( specTitle ? ( specTitle + '\n') : '' ) +
                (data.info.narrative ?: '' )
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
            data.info.allFeatures.each { FeatureInfo feature ->
                FeatureRun run = data.featureRuns.find { it.feature == feature }
                if ( run && Utils.isUnrolled( feature ) ) {
                    run.failuresByIteration.each { iteration, problems ->
                        final name = feature.iterationNameProvider.getName( iteration )
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
        if ( excludeToc.toLowerCase() != 'true' ) writeFeatureToc( builder, data )
        data.info.allFeatures.each { FeatureInfo feature ->
            FeatureRun run = data.featureRuns.find { it.feature == feature }
            if ( run && Utils.isUnrolled( feature ) ) {
                run.failuresByIteration.each { iteration, problems ->
                    final name = feature.iterationNameProvider.getName( iteration )
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
        feature.blocks.each { BlockInfo block ->
            writeBlock( builder, block, feature, iteration )
        }
    }

    private void writeBlock( MarkupBuilder builder, BlockInfo block, FeatureInfo feature, IterationInfo iteration ) {
        def trCssClassArg = ( feature.skipped ? [ 'class': 'ignored' ] : null )
        if ( !Utils.isEmptyOrContainsOnlyEmptyStrings( block.texts ) )
            block.texts.eachWithIndex { blockText, index ->
                if ( iteration ) {
                    blockText = stringProcessor.process( blockText, feature.dataVariables, iteration )
                }
                writeBlockRow( builder, trCssClassArg,
                        ( index == 0 ? block.kind : 'AND' ), blockText )
            }
        else if ( !hideEmptyBlocks )
            writeBlockRow( builder, trCssClassArg, block.kind, '----' )
    }

    private writeBlockRow( MarkupBuilder builder, cssClass, blockKind, text ) {
        builder.tr( cssClass ) {
            writeBlockKindTd( builder, blockKind )
            td {
                div( 'class': 'block-text', text )
            }
        }
    }

    private void writeBlockKindTd( MarkupBuilder builder, blockKindKey ) {
        builder.td {
            div( 'class': 'block-kind', Utils.block2String[ blockKindKey ] )
        }
    }

    private void writeRun( MarkupBuilder builder, FeatureRun run ) {
        if ( !run.feature.parameterized ) return
        builder.tr {
            writeBlockKindTd( builder, 'EXAMPLES' )
            td {
                div( 'class': 'spec-examples' ) {
                    table( 'class': 'ex-table' ) {
                        thead {
                            run.feature.parameterNames.each { param ->
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
            iteration.dataValues.each { value ->
                td( 'class': 'ex-value', value )
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
                    annotation.value().each { link ->
                        li {
                            a( 'href': link ) {
                                mkp.yield link
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
