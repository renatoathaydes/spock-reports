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
			head {}
			body {
				h1 "Report for ${data.info.description.className}"
				h2 "Specifications:"
				table {
					tbody {
						data.info.allFeatures.each { FeatureInfo feature ->
							feature.blocks.each { BlockInfo block ->
								block.texts.eachWithIndex { specText, index ->
									tr {
										td {
											def specHeader = block2String[ index == 0 ? block.kind : 'AND' ]
											span( 'class': 'spec-header', specHeader )
											span( 'class': 'spec-text', specText )
										}
									}
								}
							}
						}

						data.featureRuns.each { fRun ->
							fRun.errorsByIteration.each { iteration, errors ->
								//TODO
							}
						}
					}
				}

			}
		}
		'<!DOCTYPE html>' + writer.toString()
	}

}
