<%
    def utils = com.athaydes.spockframework.report.util.Utils;
    def fmt = new com.athaydes.spockframework.report.internal.StringFormatHelper()
    def stp = new com.athaydes.spockframework.report.internal.StringTemplateProcessor()
    def stats = utils.stats( data )
 %>
# Report for ${data.info.description.className}

##Summary

* Total Runs: ${stats.totalRuns}
* Success Rate: ${fmt.toPercentage(stats.successRate)}%
* Failures: ${stats.failures}
* Errors:   ${stats.errors}
* Skipped:  ${stats.skipped}
* Total time: ${fmt.toTimeDuration(stats.time)}

## Features

<%
    features.call { name, result, blocks, iterations ->
%>
### $name

Result: **$result**

<%
        blocks.each { block ->
 %>
* {block.kind} {block.text}
<%
        }
        iterations.each { iteration, errors ->
%>
| {iteration.dataValues.join( ' | ' )} | {errors ? '(FAIL)' : '(PASS)'}
<%      }
    }
 %>

Footer