# Spock Reports Extension
*by Renato Athaydes*

Check out [the latest news](news.md) about this project!

## What it is

This project is a global extension for [Spock](https://code.google.com/p/spock/) to create test (or, in Spock terms, Specifications) reports.

By default, the report creator generates a **HTML report** for each Specification, as well as a summary of all Specifications that have been run (index.html).

If you prefer to have your own template to generate reports from, you can use the TemplateReportCreator. This allows you
to generate reports in any text format.
See the **"Using template reports"** section below.

## Where to find demo reports

I am using [CodePen](http://codepen.io) to design the HTML [feature report](http://codepen.io/renatoathaydes/full/ihGgt), which contains detailed information about each Specification run by Spock, including the examples given (*Where* block) and their results, if any, and the [summary report](http://codepen.io/renatoathaydes/full/mKckz), which summarizes the results of all Specification runs. Click on the links to see the reports used for testing.

If you don't like the styles, you can use your own css stylesheets (see the customization section below). I welcome feedback on how to improve the report looks!

## How to use it 

To enable this Spock extension, you only need to declare a dependency to it (if using Maven, Ivy, Gradle etc) or, in other words, add the jar to the classpath.

In Maven:

Enable the JCenter repository:

```xml
    <repository>
      <id>jcenter</id>
      <name>JCenter Repo</name>
      <url>http://jcenter.bintray.com</url>
    </repository>
```

Add ``spock-reports`` to your ``<dependencies>``:

```xml
<dependency>
  <groupId>com.athaydes</groupId>
  <artifactId>spock-reports</artifactId>
  <version>1.2.4</version>
  <scope>test</scope>
</dependency>
```

In Gradle:

```groovy
repositories {
  jcenter()
}

dependencies {
    testCompile 'com.athaydes:spock-reports:1.2.4'
}
```

If you prefer, you can just download the jar directly from [JCenter](http://jcenter.bintray.com/com/athaydes/spock-reports/).

The only dependencies of this project are on Groovy (version 2.0+) and Spock, but if you're using Spock (version 0.7-groovy-2.0+), you'll already have both!


## Customizing the reports

You can provide custom configuration in a properties file located at the following location (relative to the classpath):

`META-INF/services/com.athaydes.spockframework.report.IReportCreator.properties`

Here's the default properties file:

```properties
# Name of the implementation class of the report creator
# Currently supported classes are:
#   1. com.athaydes.spockframework.report.internal.HtmlReportCreator
#   2. com.athaydes.spockframework.report.template.TemplateReportCreator
com.athaydes.spockframework.report.IReportCreator=com.athaydes.spockframework.report.internal.HtmlReportCreator

# Set properties of the report creator
# For the HtmlReportCreator, the only properties available are
# (the location of the css files is relative to the classpath):
com.athaydes.spockframework.report.internal.HtmlReportCreator.featureReportCss=spock-feature-report.css
com.athaydes.spockframework.report.internal.HtmlReportCreator.summaryReportCss=spock-summary-report.css
# exclude Specs Table of Contents
com.athaydes.spockframework.report.internal.HtmlReportCreator.excludeToc=false

# Output directory (where the spock reports will be created) - relative to working directory
com.athaydes.spockframework.report.outputDir=build/spock-reports

# If set to true, hides blocks which do not have any description
com.athaydes.spockframework.report.hideEmptyBlocks=false
```

Notice that the location of the css file is relative to the classpath!
That means that you have the freedom to place the css files in a separate jar, for example.

The output directory, on the other hand, is relative to the working directory.
For Maven projects which use the defaults, you might want to change it to `target/spock-reports`.

### Properties file location for Grails users

In Grails apps, the properties file has to be placed in `grails-app/conf/META-INF/services`.

So the full path and name for the properties should be:

`grails-app/conf/META-INF/services/com.athaydes.spockframework.report.IReportCreator.properties`

### System properties overrides

The following configuration options can also be overridden by system properties.  These system properties must be set prior to Spock being initialized (which starts this extension).  So you must ensure to set these properties as either JVM arguments or in your own bootstrapping function that in guaranteed to execute before Spock is initialized.  When set *before Spock is initialied*, these system properties will take precedence over values read from config files.  If Spock is initialized before these properties are set then they will have no effect.

`com.athaydes.spockframework.report.IReportCreator`: Set the report creator class to use.
`com.athaydes.spockframework.report.outputDir`: Set the output directory of the generated reports; relative paths are relative to the working directory.
`com.athaydes.spockframework.report.hideEmptyBlocks`: true|false; should blocks with empty text be printed out in report?

Default values are inherited from those described above.

## Using template reports

If you don't like the looks of the HTML report or want your reports in a different text format, you can use the
TemplateReportCreator to do that.

All you need to do to get started is provide a config file, as explained above, setting the `IReportCreator` to
`com.athaydes.spockframework.report.template.TemplateReportCreator`:

```properties
com.athaydes.spockframework.report.IReportCreator=com.athaydes.spockframework.report.template.TemplateReportCreator

# Set properties of the report creator
com.athaydes.spockframework.report.template.TemplateReportCreator.specTemplateFile=/templateReportCreator/spec-template.md
com.athaydes.spockframework.report.template.TemplateReportCreator.reportFileExtension=md
com.athaydes.spockframework.report.template.TemplateReportCreator.summaryTemplateFile=/templateReportCreator/summary-template.md
com.athaydes.spockframework.report.template.TemplateReportCreator.summaryFileName=summary.md

# Output directory (where the spock reports will be created) - relative to working directory
com.athaydes.spockframework.report.outputDir=build/spock-reports

# If set to true, hides blocks which do not have any description
com.athaydes.spockframework.report.hideEmptyBlocks=false
```

Just copy the above contents to a file at `META-INF/services/com.athaydes.spockframework.report.IReportCreator.properties`
relative to the classpath (eg. in `src/test/resources` for Maven users) and spock-reports will create a MD (mark-down)
report for your tests.

To provide your own template, change the location of the template file and the file extension
you wish your reports to have using the config file.

To get started with your own template, check the [existing template file](src/main/resources/templateReportCreator/spec-template.md).

You can see an example report created with the default template file [here](src/test/resources/FakeTest.md)
(this is actually used in the spock-reports tests).

### How templates work

The template report creator uses Groovy's [GStringTemplateEngine](http://groovy.codehaus.org/Groovy+Templates#GroovyTemplates-GStringTemplateEngine)
to create reports based on a template file.

This template mechanism is very simple to use, but also very powerful, as you can write any code you want in the template file.

Here's the most basic template you could imagine, which simply outputs the name of the Specification that ran:

```
This is a Report for ${data.info.description.className}
```

As you can see, you can use `${variable}` to run actual code whose result will be printed in the report.
Another way to do this, is to use `<% code %>` blocks, as in the following example, which prints the name and
result of all features in a Specification:

```
<%
    features.forEach { name, result, blocks, iterations, params ->
%>
Feature Name: $name
Result: $result
<%
    }
%>
```

You probably noticed that some variables are available to be used in code in the template file.

These variables are the following:

* `data`: an instance of `SpecData` containing the result of running a Specification.
* `reportCreator`: the `TemplateReportCreator` instance.
* `features`: as shown above, an Object which has a `forEach` method which can be used to iterate over all features of a
    Specification.

As the default template file shows, you can get statistics for the Specification easily with this code snippet:

```
<% def stats = com.athaydes.spockframework.report.util.Utils.stats( data ) %>
Report statistics: $stats
```

`stats` is a `Map` containing the following keys:

```
failures, errors, skipped, totalRuns, successRate, time
```

So, you can use it in your template like this, for example:

```
Total number of runs:   ${stats.totalRuns}
Success rate:           ${stats.successRate}
Number of failures:     ${stats.failures}
Number of errors:       ${stats.errors}
Number of ignored:      ${stats.skipped}
Total time (ms):        ${stats.time}

Created on ${new Date()} by ${System.properties['user.name']}
```

## Submitting pull requests

Please submit pull requests with bug fixes at any time!!

But if your Pull Request is about a new feature, please make sure to create an issue first so that
we can all discuss whether it's a good idea and what's the best way to go about it.

Also, please notice that the master branch is supposed to contain only releases... the development branch
is called `next`, so *all PRs should be submitted against `next`*, not master.
