package com.athaydes.spockframework.report.internal

import groovy.xml.MarkupBuilder
import groovy.xml.XmlUtil
import org.spockframework.runtime.model.ErrorInfo
import org.spockframework.runtime.model.IterationInfo

/**
 * 
 * User: Renato
 */
class ProblemBlockWriter {

	StringFormatHelper stringFormatter

	void writeProblems( MarkupBuilder builder, FeatureRun run, boolean isError ) {
		List<Map> problems = ( isError ?
			[ [ messages: [ run.error.toString() ] ] ] :
			failures( run.failuresByIteration ) )
		problems.each { Map problem ->
			if ( problem.dataValues ) {
				builder.ul {
					li {
						div problem.dataValues.toString()
						writeProblemMsgs( builder, problem.messages )
					}
				}
			} else {
				writeProblemMsgs( builder, problem.messages )
			}
		}
	}

	private void writeProblemMsgs( MarkupBuilder builder, List msgs ) {
		builder.ul {
			msgs.each { msg ->
				li {
					pre {
						mkp.yieldUnescaped(
								stringFormatter.formatToHtml(
										XmlUtil.escapeXml( msg.toString() ) ) )
					}
				}
			}
		}
	}

	private List<Map> failures( Map<IterationInfo, List<ErrorInfo>> failures ) {
		failures.inject( [ ] ) { List<Map> acc, iteration, List<ErrorInfo> failureList ->
			def errorMessages = failureList.collect { it.exception.toString() }
			if ( errorMessages ) {
				acc << [ dataValues: iteration.dataValues, messages: errorMessages ]
			}
			acc
		}
	}


}
