package com.athaydes.spockframework.report.internal

import com.athaydes.spockframework.report.IReportCreator
import groovy.xml.MarkupBuilder
import org.spockframework.runtime.model.BlockInfo
import org.spockframework.runtime.model.ErrorInfo
import org.spockframework.runtime.model.FeatureInfo
import org.spockframework.runtime.model.IterationInfo

import java.nio.file.Paths

import static org.spockframework.runtime.model.BlockKind.*

/**
 *
 * User: Renato
 */
class HtmlReportCreator implements IReportCreator {

	def reportAggregator = new HtmlReportAggregator()
	def css = ''

	final block2String = [
			( SETUP ): 'Given:',
			( CLEANUP ): 'Cleanup:',
			( THEN ): 'Then:',
			( EXPECT ): 'Expect:',
			( WHEN ): 'When:',
			( WHERE ): 'Where:',
			'AND': 'And:'
	]

	@Override
	void createReportFor( SpecData data ) {
		def specClassName = data.info.description.className
		def reportsDir = new File( 'build', 'spock-reports' )
		reportsDir.mkdirs()
		Paths.get( reportsDir.absolutePath, specClassName + '.html' )
				.toFile().write( reportFor( data ) )
	}

	String reportFor( SpecData data ) {
		def writer = new StringWriter()
		def builder = new MarkupBuilder( new IndentPrinter( new PrintWriter( writer ), "" ) )
		builder.html {
			head {
				if ( css ) style css
			}
			body {
				h1 "Report for ${data.info.description.className}"
				h2 "Specifications:"
				table {
					colgroup {
						col( 'class': 'block-kind-col' )
						col( 'class': 'block-text-col' )
					}
					tbody {
						writeSpec( builder, data )
					}
				}
			}
		}
		'<!DOCTYPE html>' + writer.toString()
	}

	private void writeSpec( MarkupBuilder builder, SpecData data ) {
		data.info.allFeatures.each { FeatureInfo feature ->
			def run = data.featureRuns.find { run -> run.feature == feature }
			writeFeatureDescription( builder, feature )
			feature.blocks.each { BlockInfo block ->
				writeBlock( builder, block, feature.skipped )
			}
			writeRun( builder, run )
		}
	}

	private void writeBlock( MarkupBuilder builder, BlockInfo block, boolean isIgnored ) {
		def trCssClassArg = ( isIgnored ? [ 'class': 'ignored' ] : null )
		block.texts.eachWithIndex { blockText, index ->
			builder.tr( trCssClassArg ) {
				writeBlockKindTd( builder, index == 0 ? block.kind : 'AND' )
				td {
					span( 'class': 'block-text', blockText )
				}
			}
		}
	}

	private void writeBlockKindTd( MarkupBuilder builder, blockKindKey ) {
		builder.td {
			span( 'class': 'block-kind', block2String[ blockKindKey ] )
		}
	}

	private void writeRun( MarkupBuilder builder, FeatureRun run ) {
		if ( !run || !run.feature.parameterized ) return
		builder.tr {
			writeBlockKindTd( builder, WHERE )
			td {
				span( 'class': 'spec-examples' ) {
					table( 'class': 'ex-table' ) {
						thead {
							run.feature.parameterNames.each { param ->
								th( 'class': 'ex-header', param )
							}
						}
						tbody {
							run.errorsByIteration.each { iteration, errors ->
								writeIteration( builder, iteration, errors )
							}
						}
					}
				}
			}
			td {
				span( 'class': 'spec-status', iterationsResult( run ) )
			}
		}

	}

	private String iterationsResult( FeatureRun run ) {
		def totalRuns = run.errorsByIteration.size()
		def totalErrors = run.errorsByIteration.values().count { !it.empty }
		"${totalRuns - totalErrors}/${totalRuns} passed"
	}

	private void writeIteration( MarkupBuilder builder, IterationInfo iteration,
	                             List<ErrorInfo> errors ) {
		builder.tr( 'class': errors ? 'ex-fail' : 'ex-pass' ) {
			iteration.dataValues.each { value ->
				td( 'class': 'ex-value', value )
			}
			td( 'class': 'ex-result', iterationResult( errors ) )
		}
	}

	private String iterationResult( List<ErrorInfo> errors ) {
		errors ? 'FAIL' : 'OK'
	}

	private void writeFeatureDescription( MarkupBuilder builder, FeatureInfo feature ) {
		def additionalCssClass = feature.skipped ? ' ignored' : ''
		builder.tr {
			td( colspan: '10' ) {
				div( 'class': 'feature-description' + additionalCssClass, feature.name )
			}
		}
	}

}
