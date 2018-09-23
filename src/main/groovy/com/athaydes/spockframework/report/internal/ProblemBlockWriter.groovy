package com.athaydes.spockframework.report.internal

import com.athaydes.spockframework.report.util.Utils
import groovy.xml.MarkupBuilder
import org.spockframework.runtime.model.IterationInfo

/**
 *
 * User: Renato
 */
class ProblemBlockWriter {

    StringFormatHelper stringFormatter
    boolean printThrowableStackTrace = false

    void writeProblemBlockForAllIterations( MarkupBuilder builder, FeatureRun run, boolean isError, boolean isFailure ) {
        if ( isError || isFailure ) {
            problemsContainer( builder ) {
                writeProblems( builder, problemsByIteration( run.failuresByIteration, run.timeByIteration ) )
            }
        }
    }

    void writeProblemBlockForIteration( MarkupBuilder builder, IterationInfo iteration,
                                        List<SpecProblem> problems, Long time ) {
        if ( problems ) {
            problemsContainer( builder ) {
                def problemsByIteration = problemsByIteration( [ ( iteration ): problems ], [ ( iteration ): time ] )
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
            if ( !problem.messages ) {
                return
            }
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

    void writeProblemMsgs( MarkupBuilder builder, List msgs ) {
        builder.ul {
            msgs.each { msg ->
                li {
                    pre {
                        mkp.yieldUnescaped(
                                stringFormatter.formatToHtml(
                                        stringFormatter.escapeXml( formatProblemMessage( msg ) ) ) )
                    }
                }
            }
        }
    }

    private static List<Map> problemsByIteration( Map<IterationInfo, List<SpecProblem>> failures,
                                                  Map<IterationInfo, Long> times ) {
        Utils.iterationData( failures, times ).collect { Map entry ->
            entry + [ messages: entry.errors ]
        }
    }

    protected String formatProblemMessage( message ) {
        if ( printThrowableStackTrace && message instanceof Throwable ) {
            def writer = new StringWriter()
            if ( message instanceof SpecInitializationError ) {
                message = message.cause
            }
            message.printStackTrace( new PrintWriter( writer ) )
            return writer.toString()
        } else if ( message == null ) {
            return 'null'
        } else {
            return message.toString()
        }
    }

}
