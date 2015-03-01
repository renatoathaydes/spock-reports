package com.athaydes.spockframework.report.internal

import groovy.xml.MarkupBuilder
import groovy.xml.XmlUtil
import org.spockframework.runtime.model.IterationInfo

/**
 *
 * User: Renato
 */
class ProblemBlockWriter {
	
	static private int problemId = 0

	StringFormatHelper stringFormatter

	void writeProblemBlockForAllIterations( MarkupBuilder builder, FeatureRun run, boolean isError, boolean isFailure ) {
		if ( isError || isFailure ) {
			problemsContainer( builder ) {
				writeProblems( builder, problemsByIteration( run.failuresByIteration ) )
			}
		}
	}

	void writeProblemBlockForIteration( MarkupBuilder builder, IterationInfo iteration, List<SpecProblem> problems ) {
		if ( problems ) {
			problemsContainer( builder ) {
				def problemsByIteration = problemsByIteration( [ ( iteration ): problems ] )
				problemsByIteration.each { it.dataValues = null } // do not show data values in the report
				writeProblems( builder, problemsByIteration )
			}
		}
	}

	void problemsContainer( MarkupBuilder builder, Runnable createProblemList ) {
		builder.tr {
			td( colspan: '10' ) {
				div( 'class': 'problem-description' ) {
					div( 'class': 'problem-header', 'The following problems occurred:' )
					div( 'class': 'problem-list' ) {
						createProblemList.run()
					}
				}
			}
		}
	}

	private void writeProblems( MarkupBuilder builder, List<Map> problems ) {
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
				def id = ++problemId
				li {
					a('class': 'problem-block', href: "#problem-$id", msg.split('\n')[0].trim())
				}
			}
		}
		for(int i = msgs.size(); i > 0; --i) {
			builder.div(id: "problem-${problemId - (i - msgs.size())}", style: 'display:none;') {
				pre {
					mkp.yieldUnescaped(stringFormatter.escapeXml(msgs[i - 1].toString()))
				}
			}
		}
	}

	private List<Map> problemsByIteration( Map<IterationInfo, List<SpecProblem>> failures ) {
		failures.inject( [ ] ) { List<Map> acc, iteration, List<SpecProblem> failureList ->
			def errorMessages = failureList.collect {
				def stack = new StringWriter()
				it.failure.exception.printStackTrace(new PrintWriter(stack))
				stack.toString()
			}
			if ( errorMessages ) {
				acc << [ dataValues: iteration.dataValues, messages: errorMessages ]
			}
			acc
		}
	}

}
