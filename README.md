# Spock Reports Extension
*by Renato Athaydes*

Check out [the latest news](news.md) about this project!

[![Build Status](https://travis-ci.org/renatoathaydes/spock-reports.svg?branch=next)](https://travis-ci.org/renatoathaydes/spock-reports)

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

Spock-reports is available on Maven Central and on JCenter!

### If you are using Maven

Add ``spock-reports`` to your ``<dependencies>``:

```xml
<dependency>
  <groupId>com.athaydes</groupId>
  <artifactId>spock-reports</artifactId>
  <version>1.2.13</version>
  <scope>test</scope>
  <!-- this avoids affecting your version of Groovy/Spock -->
  <exclusions>
    <exclusion>
      <groupId>*</groupId>
      <artifactId>*</artifactId>
    </exclusion>
  </exclusions>
</dependency>

<!-- // if you don't already have slf4j-api and an implementation of it in the classpath, add this! -->
<dependency>
  <groupId>org.slf4j</groupId>
  <artifactId>slf4j-api</artifactId>
  <version>1.7.13</version>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.slf4j</groupId>
  <artifactId>slf4j-simple</artifactId>
  <version>1.7.13</version>
  <scope>test</scope>
</dependency>
```

### If you are using Gradle

```groovy
// make sure to enable jcenter() or mavenCentral()
repositories {
  jcenter()
}

dependencies {
    testCompile( 'com.athaydes:spock-reports:1.2.13' ) {
        transitive = false // this avoids affecting your version of Groovy/Spock
    }
    // if you don't already have slf4j-api and an implementation of it in the classpath, add this!
    testCompile 'org.slf4j:slf4j-api:1.7.13'
    testCompile 'org.slf4j:slf4j-simple:1.7.13'
}
```

If you prefer, you can just download the jar directly from [JCenter](http://jcenter.bintray.com/com/athaydes/spock-reports/).

The only dependencies of this project are on Groovy (version 2.0+) and Spock, but if you're using Spock (version 0.7-groovy-2.0+), you'll already have both!


## Customizing spock-reports logging

`spock-reports` uses the `slf4j-api` for logging, so you can get logging information to investigate any issues you
may face with your Spock tests.

If your application does not have a slf4j implementation framework in the classpath, you may get this warning when running
your tests:

```
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
```

To get rid of that, add a dependency on a logging framework that logs over the slf4j-api.

For example, to use `slf4j-simple`, add this line to your Gradle dependencies (or the equivalent XML in your Maven pom):

```groovy
testCompile group: 'org.slf4j', name: 'slf4j-simple', version: '1.7.13'
```

To configure the logging framework itself, please check the documentation of the framework you decide to use.
If you're using `slf4j-simple`, check [this](http://www.slf4j.org/apidocs/org/slf4j/impl/SimpleLogger.html).

Most logging messages emitted by `spock-reports` use the `DEBUG` level, except when errors happen, in which case the
`WARN` level is used.

The base `spock-reports`'s logger name is `com.athaydes.spockframework.report`.

## Customizing the reports

You can provide custom configuration in a properties file located at the following location (relative to the classpath):

`META-INF/services/com.athaydes.spockframework.report.IReportCreator.properties`

**If you use Grails**, the above location will not work... the correct location depends on the Grails version
you're using. See the following blog posts by @rdmueller for instructions:

* [Spock Reports with Grails 2.2](https://rdmueller.github.io/Spock-Reports-with-Grails-2.2/)
* [Spock Reports with Grails 2.5](https://rdmueller.github.io/Spock-Reports-with-Grails-2.5/)
* [Spock Reports with Grails 3.0](https://rdmueller.github.io/Spock-Reports-with-Grails-3.0/)

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
com.athaydes.spockframework.report.internal.HtmlReportCreator.printThrowableStackTrace=false
com.athaydes.spockframework.report.internal.HtmlReportCreator.inlineCss=true

# exclude Specs Table of Contents
com.athaydes.spockframework.report.internal.HtmlReportCreator.excludeToc=false

# Output directory (where the spock reports will be created) - relative to working directory
com.athaydes.spockframework.report.outputDir=build/spock-reports

# If set to true, hides blocks which do not have any description
com.athaydes.spockframework.report.hideEmptyBlocks=false

# Set properties specific to the TemplateReportCreator
com.athaydes.spockframework.report.template.TemplateReportCreator.specTemplateFile=/templateReportCreator/spec-template.md
com.athaydes.spockframework.report.template.TemplateReportCreator.reportFileExtension=md
com.athaydes.spockframework.report.template.TemplateReportCreator.summaryTemplateFile=/templateReportCreator/summary-template.md
com.athaydes.spockframework.report.template.TemplateReportCreator.summaryFileName=summary.md
```

The `outputDir` property is relative to the working directory.

For Maven projects which use the defaults, you might want to change it to `target/spock-reports`.

### Customizing the report stylesheets

The CSS properties above can be set to either of the following kinds of values:

* a classpath resource.
* a URL (the value will be used to call Java's `new URL(value)`.

If the value does not match a full URL starting with a protocol (eg. `file:///usr/local/css/report.css`),
the value will be treated as an absolute path to a classpath resource.

For example, if you set the value of a CSS property to `my-css/test-report.css`, the resource `/my-css/test-report.css`
will be looked up in all Jars and directories which are part of the classpath.

If you set the value to `http://myhost.com/css/test-report.css`, the resource at this URL will be read.

#### Disabling CSS inlining

By default, the CSS resource will be inlined in the HTML report.

If you set the `inlineCss` property to `false`, then the CSS resource will be copied to the `outputDir` directory,
together with the HTML reports, with the following names:

* `feature-report.css` (for the `featureReportCss` property).
* `summary-report.css` (for the `summaryReportCss` property).

A link to the CSS resources with the above names will be added to the HTML file instead of inlining the CSS.

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

To provide your own template, change the location of the template files, the file extension
you wish your reports to have, and the name for the summary report file, using the config file.

To get started with your own template, check the [existing spec template file](src/main/resources/templateReportCreator/spec-template.md)
and the [summary template](src/main/resources/templateReportCreator/summary-template.md).

You can see an example report created with the default spec template file [here](src/test/resources/FakeTest.md)
(this is actually used in the spock-reports tests).

### How templates work

The template report creator uses Groovy's [GStringTemplateEngine](http://groovy.codehaus.org/Groovy+Templates#GroovyTemplates-GStringTemplateEngine)
to create reports based on a template file.

This template mechanism is very simple to use, but also very powerful, as you can write any code you want in the template file.

There are two templates you should provide:

* Spec report template: report for the run of a single Specification.
* Summary template: contains a summary of all Specifications that have been run during a JVM lifetime.

#### Spec report template

Here's the most basic Spec template you could imagine, which simply outputs the name of the Specification that ran:

```
This is a Report for ${utils.getSpecClassName(data)}
```

As you can see, you can use `${variable}` to run actual code whose result will be printed in the report.
Another way to do this, is to use `<% code %>` blocks, as in the following example, which prints the name and
result of all features in a Specification:

```
<%
    features.eachFeature { name, result, blocks, iterations, params ->
%>
Feature Name: $name
Result: $result
<%
    }
%>
```

> NOTE: before version 1.2.6, `eachFeature` used to be called `forEach`. This had to be changed to avoid conflict
 with Java 8's method of the same name.

You probably noticed that some variables are available to be used in code in the template file.

These variables are the following:

* `data`: an instance of [`SpecData`](src/main/groovy/com/athaydes/spockframework/report/internal/SpecData.groovy)
  containing the result of running a Specification.
* `reportCreator`: the [`TemplateReportCreator`](src/main/groovy/com/athaydes/spockframework/report/template/TemplateReportCreator.groovy) instance.
* `fmt`: an instance of [`StringFormatHelper`](src/main/groovy/com/athaydes/spockframework/report/internal/StringFormatHelper.groovy).
  It provides methods such as `String toTimeDuration( timeInMs )`
  and `String escapeXml( String str )`.
* `utils`: the [`Utils`](src/main/groovy/com/athaydes/spockframework/report/util/Utils.groovy) class, which offers
  many *useful* methods like `Map stats( SpecData data )`, which returns statistics about the given Specification.
* `features`: as shown above, an Object which has a `eachFeature` method which can be used to iterate over all features of a
  Specification. When inside the `eachFeature` closure, you can access directly all members of the current feature
  (an instance of `FeatureInfo`). So, for example, to get the `Title` annotation of a feature, you can do
  `utils.specAnnotation( data, spock.lang.Title )`.

As the default template file shows, you can get statistics for the Specification easily with this code snippet:

```
<% def stats = utils.stats( data ) %>
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

#### Summary template

The summary template has access to a single variable called `data`.
This is a Map containing all the available data for all Specifications that have been run.

For example, after running two Specifications called `test.FirstSpec` and `test.SecondSpec`,
the `data` Map could look like this:

```groovy
[ test.FirstSpec: [ failures: 1, errors: 0, skipped: 0, totalRuns: 1, successRate: 0.0, time: 159],
  test.SecondSpec: [ failures: 0, errors: 1, skipped: 0, totalRuns: 3, successRate: 0.6666666666666666, time: 8 ] ]
```

You can then iterate over each Spec data as follows:

```
<% data.each { name, map ->
 %>| $name | ${map.totalRuns} | ${map.failures} | ${map.errors} | ${map.skipped} | ${map.successRate} | ${map.time} |
<% }
 %>
```

Check the default [summary template](src/main/resources/templateReportCreator/summary-template.md) for a full example.

## Submitting pull requests

Please submit pull requests with bug fixes at any time!!

But if your Pull Request is about a new feature, please make sure to create an issue first so that
we can all discuss whether it's a good idea and what's the best way to go about it.

Also, please notice that the master branch is supposed to contain only releases... the development branch
is called `next`, so *all PRs should be submitted against `next`*, not master.
