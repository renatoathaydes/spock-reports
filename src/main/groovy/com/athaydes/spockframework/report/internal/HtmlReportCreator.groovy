package com.athaydes.spockframework.report.internal

import com.athaydes.spockframework.report.IReportCreator
import com.athaydes.spockframework.report.util.Hasher
import com.athaydes.spockframework.report.util.Utils
import com.athaydes.spockframework.report.vivid.BlockCode
import com.athaydes.spockframework.report.vivid.SpecSourceCodeReader
import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder
import org.spockframework.runtime.model.BlockInfo
import org.spockframework.runtime.model.FeatureInfo
import org.spockframework.runtime.model.IterationInfo
import org.spockframework.runtime.model.SpecElementInfo
import spock.lang.Ignore
import spock.lang.PendingFeature
import spock.lang.Title

import static com.athaydes.spockframework.report.util.Utils.featureAnnotation

/**
 *
 * User: Renato
 */
@Slf4j
class HtmlReportCreator extends AbstractHtmlCreator<SpecData>
        implements IReportCreator {

    final HtmlReportAggregator reportAggregator

    def stringFormatter = new StringFormatHelper()
    def problemWriter = new ProblemBlockWriter( stringFormatter: stringFormatter )
    def stringProcessor = new StringTemplateProcessor()
    private final SpecSourceCodeReader codeReader = new SpecSourceCodeReader()
    boolean showCodeBlocks = false
    boolean enabled = true

    HtmlReportCreator() {
        reportAggregator = new HtmlReportAggregator()
    }

    HtmlReportCreator( HtmlReportAggregator reportAggregator ) {
        this.reportAggregator = reportAggregator
    }

    @Override
    void setTestSourceRoots( roots ) {
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

    void setSpecSummaryNameOption( String option ) {
        try {
            reportAggregator?.specSummaryNameOption = SpecSummaryNameOption.valueOf( option.toUpperCase() )
        } catch ( e ) {
            log.warn( "Error trying to set specSummaryNameOption: {}", e.toString() )
        }
    }

    @Override
    void setAggregatedJsonReportDir( String dir ) {
        reportAggregator?.aggregatedJsonReportDir = dir
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
                        .write( reportFor( data ), 'UTF-8' )
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

    @Override
    void writeSummary( MarkupBuilder builder, SpecData data ) {
        builder.div( 'class': 'back-link' ) {
            a( href: 'index.html', '<< Back' )
        }
        builder.div( 'class': 'summary-report' ) {
            h3 'Summary:'
            builder.div( 'class': 'date-test-ran', whenAndWho.whenAndWhoRanTest( stringFormatter ) )
            table( 'class': 'summary-table' ) {
                thead {
                    tr {
                        th 'Executed features'
                        th 'Passed'
                        th 'Failures'
                        th 'Errors'
                        th 'Skipped'
                        th 'Success rate'
                        th 'Time'
                    }
                }
                tbody {
                    tr {
                        def stats = Utils.stats( data )
                        td stats.totalRuns
                        td stats.passed
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

    @Override
    protected void writeDetails( MarkupBuilder builder, SpecData data ) {
        def specTitle = Utils.specAnnotation( data, Title )?.value() ?: ''
        if ( specTitle ) {
            builder.pre( 'class': 'title', specTitle )
        }
        def narrative = data.info.narrative ?: ''
        if ( narrative ) {
            builder.pre( 'class': 'narrative', narrative )
        }
        def pendingFeature = Utils.specAnnotation( data, PendingFeature )
        if ( pendingFeature ) {
            writePendingFeature( builder, pendingFeature )
        }
        def headers = Utils.specHeaders( data )
        if ( headers ) {
            writeHeaders( builder, headers )
        }
        writeTagOrAttachment( builder, data.info )
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
                FeatureRun run = data.withFeatureRuns { it.find { it.feature == feature } }
                if ( run && Utils.isUnrolled( feature ) ) {
                    def multipleIterations = run.failuresByIteration.size() > 1
                    run.failuresByIteration.eachWithIndex { iteration, problems, int index ->
                        final String name = Utils.featureNameFrom( feature, iteration, index, multipleIterations )
                        final cssClass = problems.any( Utils.&isError ) ? 'error' :
                                problems.any( Utils.&isFailure ) ? 'failure' :
                                        Utils.isSkipped( feature ) ? 'ignored' : 'pass'
                        li {
                            a( href: "#${Hasher.instance.hash( name )}", 'class': "feature-toc-$cssClass", name )
                        }
                    }
                } else {
                    final failures = run ? Utils.countProblems( [ run ], Utils.&isFailure ) : 0
                    final errors = run ? Utils.countProblems( [ run ], Utils.&isError ) : 0
                    final cssClass = errors ? 'error' : failures ? 'failure' : !run ? 'ignored' : 'pass'
                    li {
                        a( href: "#${Hasher.instance.hash( feature.name )}", 'class': "feature-toc-$cssClass", feature.name )
                    }
                }
            }
        }
    }

    private void writeFeature( MarkupBuilder builder, SpecData data ) {
        if ( data.initializationError ) {
            problemWriter.problemsContainer( builder, 'Initialization error:' ) {
                problemWriter.writeProblemMsgs( builder, [
                        data.initializationError.exception
                ] )
            }
        }

        if ( excludeToc.toLowerCase() != 'true' ) writeFeatureToc( builder, data )

        for ( FeatureInfo feature in data.info.allFeaturesInExecutionOrder ) {
            FeatureRun run = data.withFeatureRuns { it.find { it.feature == feature } }
            if ( run && Utils.isUnrolled( feature ) ) {
                def iterations = run.copyFailuresByIteration().keySet().toList().sort { it.iterationIndex }
                for ( iteration in iterations ) {
                    def problems = run.failuresByIteration[ iteration ]
                    def index = iteration.iterationIndex
                    def extraInfo = Utils.nextSpecExtraInfo( data, feature, iteration )
                    String name = Utils.featureNameFrom( feature, iteration, index, iterations.size() > 1 )
                    final cssClass = problems.any( Utils.&isError ) ? 'error' :
                            problems.any( Utils.&isFailure ) ? 'failure' :
                                    Utils.isSkipped( feature ) ? 'ignored' : ''
                    def time = run.timeByIteration.get( iteration, 0L )
                    writeFeatureDescription( builder, name, cssClass,
                            featureAnnotation( feature, Ignore ),
                            featureAnnotation( feature, PendingFeature ),
                            extraInfo,
                            run.feature,
                            time )
                    writeFeatureBlocks( builder, feature, problems, iteration )
                    writeRun( builder, run, iteration )
                    problemWriter.writeProblemBlockForIteration( builder, iteration, problems, time )
                }
            } else {
                final failures = run ? Utils.countProblems( [ run ], Utils.&isFailure ) : 0
                final errors = run ? Utils.countProblems( [ run ], Utils.&isError ) : 0
                final cssClass = errors ? 'error' :
                        failures ? 'failure' :
                                ( !run || Utils.isSkipped( feature ) ) ? 'ignored' : ''

                List<IterationInfo> iterations = run
                        ? run.copyFailuresByIteration().keySet()
                        .toList().sort { it.iterationIndex }
                        : Collections.<IterationInfo> emptyList()
                def extraInfo = Collections.emptyList()
                def multipleIterations = iterations.size() > 1
                if ( run && multipleIterations ) {
                    extraInfo = iterations.collectMany {
                        Utils.nextSpecExtraInfo( data, feature, it )
                    }
                } else if ( run ) {
                    extraInfo = ( 1..run.failuresByIteration.size() ).collectMany {
                        Utils.nextSpecExtraInfo( data, feature )
                    }
                }

                Long time = run == null ? null : run.timeByIteration.values().sum()

                writeFeatureDescription( builder, feature.name, cssClass,
                        featureAnnotation( feature, Ignore ),
                        featureAnnotation( feature, PendingFeature ),
                        extraInfo,
                        run?.feature,
                        time
                )
                List<SpecProblem> problems = run ? run.copyFailuresByIteration().values().collectMany { it } : [ ]
                problems.sort { it.failure.method?.iteration?.iterationIndex ?: 0 }
                writeFeatureBlocks( builder, feature, problems )
                if ( run ) {
                    writeRun( builder, run )
                    problemWriter.writeProblemBlockForAllIterations( builder, run, errors > 0, failures > 0 )
                }
            }
        }

        if ( data.cleanupSpecError ) {
            problemWriter.problemsContainer( builder, 'Cleanup Error:' ) {
                problemWriter.writeProblemMsgs( builder, [
                        data.cleanupSpecError.exception
                ] )
            }
        }
    }


    private void writeFeatureBlocks( MarkupBuilder builder,
                                     FeatureInfo feature,
                                     List<SpecProblem> problems,
                                     IterationInfo iteration = null ) {
        if ( showCodeBlocks ) {
            boolean ok = writeBlocksFromCode( builder, feature, iteration, problems )
            if ( !ok ) {
                log.debug( "Could not find block source code, falling back on original text information" )
                writeBlocks( builder, feature, iteration )
            }
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

    private boolean writeBlocksFromCode( MarkupBuilder builder,
                                         FeatureInfo feature,
                                         IterationInfo iteration,
                                         List<SpecProblem> problems ) {
        def blocks = codeReader.getBlocks( feature )

        if ( blocks.empty ) {
            return false // unable to find sources
        }

        def failureLineNumber = -1
        def specName = Utils.getSpecClassName( feature.spec )

        if ( problems ) {
            def cause = problems.first().failure.exception
            while ( cause.cause ) {
                cause = cause.cause
            }

            def stackTraceItem = cause.stackTrace.find { it.className == specName }
            if ( stackTraceItem ) {
                failureLineNumber = stackTraceItem.lineNumber
            }
        }

        for ( BlockCode block in blocks ) {
            def text = iteration && block.text ?
                    stringProcessor.process( block.text, feature.dataVariables, iteration ) :
                    ( block.text ?: '' )
            def blockKind = block.label ?: 'Block:'
            writeBlockRowsFromCode( builder, trCssClass( feature ), blockKind, block, text, failureLineNumber )
        }

        true // found sources
    }

    private trCssClass( FeatureInfo feature ) {
        Utils.isSkipped( feature ) ? [ 'class': 'ignored' ] : null
    }

    private writeBlockRow( MarkupBuilder builder, cssClass, blockKind, text ) {
        builder.tr( cssClass ) {
            writeBlockKindTd( builder, blockKind )
            writeBlockTextTd( builder, text )
        }
    }

    private writeBlockRowsFromCode( MarkupBuilder builder, cssClass, blockKind,
                                    BlockCode code, text, int failureLineNumber ) {
        def statements = ( blockKind == 'where' ? [ ] : code.statements )
        def lineNumbers = code.lineNumbers

        if ( text ) {
            writeBlockRow( builder, cssClass, blockKind, text )
            if ( statements ) builder.tr {
                td()
                writeCodeTd( builder, statements, lineNumbers, failureLineNumber )
            }
        } else if ( statements ) builder.tr( cssClass ) {
            writeBlockKindTd( builder, blockKind )
            writeCodeTd( builder, statements, lineNumbers, failureLineNumber )
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

    private writeCodeTd( MarkupBuilder builder,
                         List<String> statements,
                         List<Integer> lineNumbers,
                         int failureLineNumber ) {
        def isPostError = failureLineNumber > 0 && failureLineNumber < lineNumbers.min()
        def isPreError = failureLineNumber > 0 && failureLineNumber > lineNumbers.max()

        if ( failureLineNumber < 0 || isPreError || isPostError ) {
            def cssClasses = [ 'block-source' ]
            if ( isPreError ) {
                cssClasses << 'pre-error'
            } else if ( isPostError ) {
                cssClasses << 'post-error'
            }
            builder.td {
                pre( 'class': cssClasses.join( ' ' ), statements.join( '\n' ) )
            }
        } else {
            List<Map> sourceLines = [ ]
            boolean foundFailure = false

            def numberedStatements = [ statements, lineNumbers ].transpose()
            numberedStatements.eachWithIndex { entry, int index ->
                def ( String statement, Integer lineNumber ) = entry
                if ( !foundFailure ) {
                    def nextLineNumber = ( index < lineNumbers.size() - 1 ) ? lineNumbers[ index + 1 ] : -1
                    def failedLine = ( lineNumber == failureLineNumber || nextLineNumber > failureLineNumber )
                    sourceLines << [ line: statement, failure: failedLine ]
                    foundFailure = failedLine
                } else {
                    sourceLines << [ line: statement, failure: false ]
                }
            }

            builder.td {
                def cssClasses = [ 'block-source' ]
                if ( foundFailure ) {
                    cssClasses << 'error'
                    def failureIndex = sourceLines.findIndexOf { it.failure }
                    def beforeFailure = failureIndex == 0 ?
                            [ ] :
                            sourceLines[ 0..<failureIndex ].collect { it.line }
                    def failure = sourceLines[ failureIndex ].line
                    def afterFailure = failureIndex < sourceLines.size() - 1 ?
                            sourceLines[ ( failureIndex + 1 )..<sourceLines.size() ].collect { it.line } :
                            [ ]
                    if ( beforeFailure ) {
                        pre( 'class': 'block-source before-error', beforeFailure.join( '\n' ) )
                    }
                    pre( 'class': 'block-source error', failure + ' // line ' + failureLineNumber )
                    if ( afterFailure ) {
                        pre( 'class': 'block-source after-error', afterFailure.join( '\n' ) )
                    }
                } else {
                    pre( 'class': 'block-source', statements.join( '\n' ) )
                }
            }
        }
    }

    private void writeRun( MarkupBuilder builder, FeatureRun run, IterationInfo iterationInfo = null ) {
        if ( !run.feature.parameterized ) return
        builder.tr {
            writeBlockKindTd( builder, 'examples' )
            td {
                div( 'class': 'spec-examples' ) {
                    table( 'class': 'ex-table' ) {
                        thead {
                            tr {
                                for ( param in run.feature.parameterNames ) {
                                    th( 'class': 'ex-header', param )
                                }
                            }
                        }
                        tbody {
                            if ( iterationInfo ) {
                                writeIteration( builder, iterationInfo,
                                        run.timeByIteration[ iterationInfo ],
                                        run.failuresByIteration[ iterationInfo ] )
                            } else {
                                run.copyFailuresByIteration().each { iteration, errors ->
                                    writeIteration( builder, iteration, run.timeByIteration[ iteration ], errors )
                                }
                            }
                        }
                    }
                }
            }
            if ( !iterationInfo ) {
                td {
                    div( 'class': 'spec-status', Utils.iterationsResult( run ) )
                }
            }
        }

    }

    private void writeIteration( MarkupBuilder builder, IterationInfo iteration,
                                 Long time,
                                 List<SpecProblem> errors ) {
        builder.tr( 'class': errors ? 'ex-fail' : 'ex-pass' ) {
            for ( value in iteration.dataValues ) {
                def writableValue = value instanceof Runnable ?
                        "<executable>" :
                        ( value == null ? '<null>' : value.toString() )
                td( 'class': 'ex-value', writableValue )
            }
            td( 'class': 'ex-result' ) {
                span( iterationResult( errors ) )
                span( 'class': 'ex-time', time == null ? "" : "(${stringFormatter.toTimeDuration( time )})" )
            }
        }
    }

    private static String iterationResult( List<SpecProblem> errors ) {
        errors ? 'FAIL' : 'OK'
    }

    private void writeFeatureDescription( MarkupBuilder builder, String name,
                                          String cssClass,
                                          Ignore ignoreAnnotation,
                                          PendingFeature pendingFeature,
                                          List extraInfo,
                                          SpecElementInfo feature,
                                          Long time = null ) {
        def ignoreReason = ''
        if ( cssClass == 'ignored' && ignoreAnnotation ) {
            ignoreReason = ignoreAnnotation.value()
        }

        cssClass = cssClass ? ' ' + cssClass : ''

        builder.tr {
            td( colspan: '10' ) {
                div( 'class': 'feature-description' + cssClass, id: Hasher.instance.hash( name ) ) {
                    span name
                    writeLinkBackToTopAndTime builder, time
                    if ( ignoreReason ) {
                        div()
                        span( 'class': 'reason', ignoreReason )
                    }
                    writePendingFeature( builder, pendingFeature )
                    writeExtraInfo( builder, extraInfo )
                    if ( feature ) {
                        writeTagOrAttachment builder, feature
                    }
                }
            }
        }
    }

    private void writeTagOrAttachment( MarkupBuilder builder, SpecElementInfo feature ) {
        if ( feature.attachments.isEmpty() && feature.tags.isEmpty() ) {
            return
        }

        def listItem = { value ->
            builder.li {
                if ( Utils.isUrl( value.url ) ) {
                    a( 'href': value.url, value.name )
                } else {
                    span stringFormatter.escapeXml( value.name )
                }
            }
        }

        builder.div( 'class': 'issues' ) {
            if ( !feature.tags.isEmpty() ) {
                def tagsByKey = feature.tags.groupBy( { t -> t.key } )
                tagsByKey.each { key, tags ->
                    div( key.capitalize() + 's:' )
                    ul {
                        for ( tag in tags ) {
                            listItem tag
                        }
                    }
                }
            }

            if ( !feature.attachments.isEmpty() ) {
                div( 'See:' )
                ul {
                    for ( attachment in feature.attachments ) {
                        listItem attachment
                    }
                }
            }
        }
    }

    private void writeHeaders( MarkupBuilder builder, List headers ) {
        builder.div( 'class': 'spec-headers' ) {
            headers.each { header ->
                builder.div( 'class': 'spec-header' ) {
                    mkp.yieldUnescaped( header?.toString() ?: 'null' )
                }
            }
        }
    }

    private void writeExtraInfo( MarkupBuilder builder, List extraInfo ) {
        if ( extraInfo ) {
            builder.div( 'class': 'extra-info' ) {
                ul {
                    extraInfo.each { info ->
                        li {
                            div { mkp.yieldUnescaped( info?.toString() ?: 'null' ) }
                        }
                    }
                }
            }
        }
    }

    private void writePendingFeature( MarkupBuilder builder,
                                      PendingFeature annotation ) {
        if ( annotation ) {
            builder.div( 'class': 'pending-feature', 'Pending Feature' )
        }
    }

    private void writeLinkBackToTopAndTime( MarkupBuilder builder, Long time = null ) {
        builder.span( 'class': 'return-toc' ) {
            a( href: '#toc', 'Return' )
            if ( time != null ) {
                div( 'class': 'ex-time', "(${stringFormatter.toTimeDuration( time )})" )
            }
        }
    }

}
