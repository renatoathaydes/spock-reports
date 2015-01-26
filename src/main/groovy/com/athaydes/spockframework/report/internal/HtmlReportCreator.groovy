package com.athaydes.spockframework.report.internal

import static com.athaydes.spockframework.report.internal.FailureKind.ERROR
import static com.athaydes.spockframework.report.internal.FailureKind.FAILURE
import static org.spockframework.runtime.model.BlockKind.*
import groovy.util.logging.Log
import groovy.xml.MarkupBuilder

import java.util.logging.Level

import org.spockframework.runtime.model.BlockInfo
import org.spockframework.runtime.model.FeatureInfo
import org.spockframework.runtime.model.IterationInfo

import spock.lang.Unroll

import com.athaydes.spockframework.report.IReportCreator

/**
 *
 * User: Renato
 */
@Log
class HtmlReportCreator extends AbstractHtmlCreator<SpecData>
		implements IReportCreator {

	def reportAggregator = HtmlReportAggregator.instance
	def stringFormatter = new StringFormatHelper()
	def problemWriter = new ProblemBlockWriter( stringFormatter: stringFormatter )
    def stringProcessor = new StringTemplateProcessor()

	final block2String = [
			( SETUP )  : 'Given:',
			( CLEANUP ): 'Cleanup:',
			( THEN )   : 'Then:',
			( EXPECT ) : 'Expect:',
			( WHEN )   : 'When:',
			( WHERE )  : 'Where:',
			'AND'      : 'And:',
			'EXAMPLES' : 'Examples:'
	]

	void setFeatureReportCss( String css ) {
		super.setCss( css )
	}

	void setSummaryReportCss( String css ) {
		reportAggregator?.css = css
	}

	@Override
	void createReportFor( SpecData data ) {
		def specClassName = data.info.description.className
        def reportsDir = createReportsDir()
		if ( reportsDir.isDirectory() ) {
			try {
				new File( reportsDir, specClassName + '.html' )
						.write( reportFor( data ) )
			} catch ( e ) {
				log.log(Level.FINE, "${this.class.name} failed to create report for $specClassName", e)
			}

		} else {
			log.fine "${this.class.name} cannot create output directory: ${reportsDir.absolutePath}"
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
						def stats = stats( data )
						td stats.totalRuns
						td stats.failures
						td stats.errors
						td stats.skipped
						td stringFormatter.toPercentage( stats.successRate )
						td stringFormatter.toTimeDuration( stats.time )
						reportAggregator?.aggregateReport( data.info.description.className, stats, outputDir )
					}
				}
			}
		}
	}

	static int countProblems( List<FeatureRun> runs, Closure problemFilter ) {
		runs.inject( 0 ) { int count, FeatureRun fr ->
			def allProblems = fr.failuresByIteration.values().flatten()
			count + ( isUnrolled( fr.feature ) ?
					allProblems.count( problemFilter ) :
					allProblems.any( problemFilter ) ? 1 : 0 )
		}
	}

	static int countFeatures( List<FeatureRun> runs, Closure featureFilter = { true } ) {
		runs.findAll( featureFilter ).inject( 0 ) { int count, FeatureRun fr ->
			count + ( isUnrolled( fr.feature ) ? fr.iterationCount() : 1 )
		}
	}

	protected Map stats( SpecData data ) {
		def failures = countProblems( data.featureRuns, HtmlReportCreator.&isFailure )
		def errors = countProblems( data.featureRuns, HtmlReportCreator.&isError )
		def skipped = data.info.allFeatures.count { FeatureInfo f -> f.skipped }
		def total = countFeatures( data.featureRuns )
		def successRate = successRate( total, ( errors + failures ).toInteger() )
		[ failures   : failures, errors: errors, skipped: skipped, totalRuns: total,
		  successRate: successRate, time: data.totalTime ]
	}

	protected void writeDetails( MarkupBuilder builder, SpecData data ) {
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

	static boolean isUnrolled( FeatureInfo feature ) {
		feature.description?.annotations?.any { it.annotationType() == Unroll } ?: false
	}

	static boolean isFailure( SpecProblem problem ) {
		problem.kind == FAILURE
	}

	static boolean isError( SpecProblem problem ) {
		problem.kind == ERROR
	}

	private void writeFeatureToc( MarkupBuilder builder, SpecData data ) {
		builder.ul(id: 'toc') {
			data.info.allFeatures.each { FeatureInfo feature ->
				FeatureRun run = data.featureRuns.find { it.feature == feature }
				if ( run && isUnrolled( feature ) ) {
					run.failuresByIteration.each { iteration, problems ->
						final name = feature.iterationNameProvider.getName( iteration )
						final cssClass = problems.any( HtmlReportCreator.&isError ) ? 'error' :
								problems.any( HtmlReportCreator.&isFailure ) ? 'failure' :
										feature.skipped ? 'ignored' : 'pass'
						li {
							a(href: "#${name.hashCode()}", 'class': "feature-toc-$cssClass", name)
						}
					}
				} else {
					final failures = run ? countProblems( [ run ], HtmlReportCreator.&isFailure ) : 0
					final errors = run ? countProblems( [ run ], HtmlReportCreator.&isError ) : 0
					final cssClass = errors ? 'error' : failures ? 'failure' : !run ? 'ignored' : 'pass'
					li {
						a(href: "#${feature.name.hashCode()}", 'class': "feature-toc-$cssClass", feature.name)
					}
				}
			}
		}
	}
	
	private void writeFeature( MarkupBuilder builder, SpecData data ) {
		writeFeatureToc( builder, data )
		data.info.allFeatures.each { FeatureInfo feature ->
			FeatureRun run = data.featureRuns.find { it.feature == feature }
			if ( run && isUnrolled( feature ) ) {
				run.failuresByIteration.each { iteration, problems ->
					final name = feature.iterationNameProvider.getName( iteration )
                    final cssClass = problems.any( HtmlReportCreator.&isError ) ? 'error' :
                            problems.any( HtmlReportCreator.&isFailure ) ? 'failure' :
                                    feature.skipped ? 'ignored' : ''
                    writeFeatureDescription( builder, name, cssClass )
					writeFeatureBlocks( builder, feature, iteration )
					problemWriter.writeProblemBlockForIteration( builder, iteration, problems )
				}
			} else {
				final failures = run ? countProblems( [ run ], HtmlReportCreator.&isFailure ) : 0
				final errors = run ? countProblems( [ run ], HtmlReportCreator.&isError ) : 0
				final cssClass = errors ? 'error' : failures ? 'failure' : !run ? 'ignored' : ''
                writeFeatureDescription( builder, feature.name, cssClass )
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
		if ( !isEmptyOrContainsOnlyEmptyStrings( block.texts ) )
			block.texts.eachWithIndex { blockText, index ->
                if (iteration) {
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

	protected boolean isEmptyOrContainsOnlyEmptyStrings( List<String> strings ) {
		!strings || strings.every { it.trim() == '' }
	}

	private void writeBlockKindTd( MarkupBuilder builder, blockKindKey ) {
		builder.td {
			div( 'class': 'block-kind', block2String[ blockKindKey ] )
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
				div( 'class': 'spec-status', iterationsResult( run ) )
			}
		}

	}

	private String iterationsResult( FeatureRun run ) {
		def totalErrors = run.failuresByIteration.values().count { !it.empty }
		"${run.iterationCount() - totalErrors}/${run.iterationCount()} passed"
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

	private String iterationResult( List<SpecProblem> errors ) {
		errors ? 'FAIL' : 'OK'
	}

	private void writeFeatureDescription( MarkupBuilder builder, String name, String cssClass ) {
		cssClass = cssClass ? ' ' + cssClass : ''
		builder.tr {
			td( colspan: '10' ) {
				div( 'class': 'feature-description' + cssClass, id: name.hashCode(), name ) {
					span(style: 'float: right; font-size: 60%;') {
						a(href: '#toc', 'Return')
					}
				}
			}
		}
	}

}
