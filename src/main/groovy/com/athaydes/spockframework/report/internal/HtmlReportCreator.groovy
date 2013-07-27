package com.athaydes.spockframework.report.internal

import com.athaydes.spockframework.report.IReportCreator
import groovy.xml.MarkupBuilder
import org.spockframework.runtime.model.BlockInfo
import org.spockframework.runtime.model.FeatureInfo

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
						write( builder, data )
					}
				}
			}
		}
		'<!DOCTYPE html>' + writer.toString()
	}

	private void write( MarkupBuilder builder, SpecData data ) {
		def lastFeatureIndex = data.info.allFeatures.size() - 1
		data.info.allFeatures.eachWithIndex { FeatureInfo feature, index ->
			feature.blocks.each { BlockInfo block ->
				write( builder, block )
			}

			writeRun( builder, data.featureRuns.find { run -> run.feature == feature } )

			if ( index < lastFeatureIndex )
				writeFeatureSeparator( builder )
		}
	}

	private void write( MarkupBuilder builder, BlockInfo block ) {
		block.texts.eachWithIndex { blockText, index ->
			builder.tr {
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
								tr( 'class': errors ? 'ex-fail' : 'ex-pass' ) {
									iteration.dataValues.each { value ->
										td( 'class': 'ex-value', value )
									}
								}
							}
						}
					}
				}
			}
		}

	}

	private void writeFeatureSeparator( MarkupBuilder builder ) {
		builder.tr {
			td( colspan: '2' ) {
				div( 'class': 'feature-separator' )
			}
		}
	}

}
