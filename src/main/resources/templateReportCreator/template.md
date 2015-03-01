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
    unrolledFeatures.call { feature, name, result, iteration, problems ->
%>
### $name

Result: **$result**

<%
    feature.blocks.each { block ->
        if ( !hideEmptyBlocks || !utils.isEmptyOrContainsOnlyEmptyStrings( block.texts ) ) {
 %>
* ${utils.block2String[ block.kind ]}
  * ${block.texts.join('\n  * ')}
<%
        }
    }
%>

<%
    }
    regularFeatures.call { feature, name, result, run ->
%>
### $name

Result: **$result**

<%
    feature.blocks.each { block ->
        if ( !hideEmptyBlocks || !utils.isEmptyOrContainsOnlyEmptyStrings( block.texts ) ) {
 %>
* ${utils.block2String[ block.kind ]}
  * ${block.texts.join('\n  * ')}
<%
        }
    }

    if ( run && run.feature.parameterized ) {
%>

| ${run.feature.parameterNames.join( ' | ' )} |
|-------------------------------------------|
<% run.failuresByIteration.each { iteration, errors -> %>| ${iteration.dataValues.join( ' | ' )} |
<% } %>


<%
    }
  }
%>

Footer