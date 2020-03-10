# Spock Reports Extension
*by Renato Athaydes*

Check out [the latest news](news.md) about this project!

[![Actions Status](https://github.com/renatoathaydes/spock-reports/workflows/Build%20And%20Test%20on%20All%20OSs/badge.svg)](https://github.com/renatoathaydes/spock-reports/actions)
[ ![Download](https://api.bintray.com/packages/renatoathaydes/maven/spock-reports/images/download.svg) ](https://bintray.com/renatoathaydes/maven/spock-reports/_latestVersion)

## What it is

This project is a global extension for [Spock](https://code.google.com/p/spock/) to create test (or, in Spock terms, Specifications) reports.

By default, the report creator generates a **HTML report** for each Specification, as well as a summary of all Specifications that have been run (index.html).

If you prefer to have your own template to generate reports from, you can use the TemplateReportCreator. This allows you
to generate reports in any text format.
See the **"Using template reports"** section below.

> Support for Geb tests: if you use [Geb](http://gebish.org/) for web testing, check out the
  [geb-spock-reports](https://github.com/AOEpeople/geb-spock-reports) extension which adds screenshots to Spock reports.

## Where to find demo reports

I am using [CodePen](http://codepen.io) to design the HTML [feature report](http://codepen.io/renatoathaydes/full/ihGgt), which contains detailed information about each Specification run by Spock, including the examples given (*Where* block) and their results, if any, and the [summary report](http://codepen.io/renatoathaydes/full/mKckz), which summarizes the results of all Specification runs. Click on the links to see the reports used for testing.

If you don't like the styles, you can use your own css stylesheets (see the customization section below). I welcome feedback on how to improve the report looks!

## How to use it

To enable this Spock extension, you only need to declare a dependency to it (if using Maven, Ivy, Gradle etc) or, just add the jar to the project's classpath.

Spock-reports is available on Maven Central and on JCenter.

> Since version 1.3.2, Spock version 1.1+ is required.
> From version 1.7.0, Spock 1.2-groovy-2.5 or newer should be used.
> If you use Java 9+, use the latest versions of both Spock and spock-reports.

If you want to add information to your Spock-reports programmatically, since version 1.4.0, you can use the following
`Specification` class' extension methods which are added by Spock Reports:

* `void reportHeader( arg )` - dynamically insert data into the top of the Specification report.
* `void reportInfo( arg )` - add data to the feature's section.

These methods are added as
[Groovy extensions](http://docs.groovy-lang.org/docs/next/html/documentation/core-metaprogramming.html#module-descriptor),
so your IDE should be able to show them in auto-completion!

For example, you could do something like this within your `Specification`:

```groovy
def setupSpec() {
    reportHeader "<h2>Browser: ${driver.browser.name}</h2>"
}

def "My feature"() {
    expect:
    reportInfo "Some information I want to show in the report"
}
```

### If you are using Maven

Add ``spock-reports`` to your ``<dependencies>``:

```xml
<dependency>
  <groupId>com.athaydes</groupId>
  <artifactId>spock-reports</artifactId>
  <version>1.7.1</version>
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
    testCompile( 'com.athaydes:spock-reports:1.7.1' ) {
        transitive = false // this avoids affecting your version of Groovy/Spock
    }
    // if you don't already have slf4j-api and an implementation of it in the classpath, add this!
    testCompile 'org.slf4j:slf4j-api:1.7.13'
    testCompile 'org.slf4j:slf4j-simple:1.7.13'
}
```

If you prefer, you can just download the jar directly from [JCenter](http://jcenter.bintray.com/com/athaydes/spock-reports/).

The only dependencies this project has are Groovy version 2.0+ (only the
`groovy`, `groovy-templates`, `groovy-xml` and `groovy-json`
modules are required) and `Spock`, but if you're using Spock (version 0.7-groovy-2.0+) then you already have them all!

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

To get rid of the warning, add a dependency on a logging framework that implements the slf4j-api.

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

Spock-reports can be configured via the [Spock config file](http://spockframework.org/spock/docs/1.1/extensions.html#_spock_configuration_file),
a dedicated properties file or system properties.

All properties listed below are supported either way.

### Using SpockConfig.groovy

> supported since version 1.5.0

All properties for spock-reports should be added in the `spockReports` block.
To set properties, use the following syntax:

```groovy
spockReports {
    set 'com.athaydes.spockframework.report.showCodeBlocks': true
    set 'com.athaydes.spockframework.report.outputDir': 'target/spock-reports'
}
```

Alternatively:

```groovy
spockReports {
    // set all properties at once
    set( [ 'com.athaydes.spockframework.report.showCodeBlocks': true,
           'com.athaydes.spockframework.report.outputDir': 'target/spock-reports' ] )
}
```

### Using a properties file

If you prefer to use a properties file, the file should be located at the following location (relative to the classpath):

`META-INF/services/com.athaydes.spockframework.report.IReportCreator.properties`

**If you use Grails**, the above location will not work... the correct location depends on the Grails version
you're using. See the following blog posts by @rdmueller for instructions:

* [Spock Reports with Grails 2.2](https://rdmueller.github.io/Spock-Reports-with-Grails-2.2/)
* [Spock Reports with Grails 2.5](https://rdmueller.github.io/Spock-Reports-with-Grails-2.5/)
* [Spock Reports with Grails 3.0](https://rdmueller.github.io/Spock-Reports-with-Grails-3.0/)

### Using system properties

If you use Gradle and prefer system properties, they should be configured on the test task, e.g.:
```groovy
task('functionalTest', type: Test) {
  systemProperty 'com.athaydes.spockframework.report.outputDir', 'build/reports/spock'
}
```

If you use Maven and prefer system properties, they should be configured as 
[`systemPropertyVariables` in the `configuration` section](https://maven.apache.org/surefire/maven-surefire-plugin/examples/system-properties.html)
 of the `failsafe` and `surefire` plugins.

### Default properties' values

```properties
# Name of the implementation class(es) of report creator(s) to enable (separate multiple entries with commas)
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
com.athaydes.spockframework.report.internal.HtmlReportCreator.enabled=true
# options are: "class_name_and_title", "class_name", "title"
com.athaydes.spockframework.report.internal.HtmlReportCreator.specSummaryNameOption=class_name_and_title

# exclude Specs Table of Contents
com.athaydes.spockframework.report.internal.HtmlReportCreator.excludeToc=false

# Output directory (where the spock reports will be created) - relative to working directory
com.athaydes.spockframework.report.outputDir=build/spock-reports

# Output directory where to store the aggregated JSON report (used to support parallel builds)
com.athaydes.spockframework.report.aggregatedJsonReportDir=

# If set to true, hides blocks which do not have any description
com.athaydes.spockframework.report.hideEmptyBlocks=false

# Set the name of the project under test so it can be displayed in the report
com.athaydes.spockframework.report.projectName=

# Set the version of the project under test so it can be displayed in the report
com.athaydes.spockframework.report.projectVersion=Unknown

# Show the source code for each block
com.athaydes.spockframework.report.showCodeBlocks=false

# Set the root location of the Spock test source code (only used if showCodeBlocks is 'true')
com.athaydes.spockframework.report.testSourceRoots=src/test/groovy

# Set properties specific to the TemplateReportCreator
com.athaydes.spockframework.report.template.TemplateReportCreator.specTemplateFile=/templateReportCreator/spec-template.md
com.athaydes.spockframework.report.template.TemplateReportCreator.reportFileExtension=md
com.athaydes.spockframework.report.template.TemplateReportCreator.summaryTemplateFile=/templateReportCreator/summary-template.md
com.athaydes.spockframework.report.template.TemplateReportCreator.summaryFileName=summary.md
com.athaydes.spockframework.report.template.TemplateReportCreator.enabled=true
```

### Notes on `outputDir`

Be aware that the `outputDir` property is relative to the working directory.

For Maven projects which use the defaults, you might want to change the `outputDir` to `target/spock-reports`.

If your build system can cache build outputs (to, for example, skip unnecessary build steps when in/out do not change),
register the `outputDir` as an output of the `test` task.

In Gradle, for example, this can be accomplished with the following declaration in your `build.gradle` file:

```groovy
test {
    // set to the same value as 'com.athaydes.spockframework.report.outputDir'
    outputs.dir "$buildDir/spock-reports"
}
```

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

## Using template reports

If you don't like the looks of the HTML report, or want your reports in a different text format, you can use the
TemplateReportCreator to do that.

All you need to do to get started is provide a config file, or system properties, as explained above that
set the `IReportCreator` to `com.athaydes.spockframework.report.template.TemplateReportCreator`:

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

The template report creator uses Groovy's [GStringTemplateEngine](http://docs.groovy-lang.org/next/html/documentation/template-engines.html#_gstringtemplateengine)
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

You probably noticed the use of some predefined variables in the template file code example. The available variables are:

* `data`: an instance of [`SpecData`](src/main/groovy/com/athaydes/spockframework/report/internal/SpecData.groovy)
  containing the result of running a Specification.
* `reportCreator`: the [`TemplateReportCreator`](src/main/groovy/com/athaydes/spockframework/report/template/TemplateReportCreator.groovy) instance.
* `fmt`: an instance of [`StringFormatHelper`](src/main/groovy/com/athaydes/spockframework/report/internal/StringFormatHelper.groovy).
  It provides methods such as `String toTimeDuration( timeInMs )`
  and `String escapeXml( String str )`.
* `utils`: the [`Utils`](src/main/groovy/com/athaydes/spockframework/report/util/Utils.groovy) class offers
  many *useful* methods like `Map stats( SpecData data )`, which returns statistics about the given Specification.
* `features`: as shown above, is an Object whose `eachFeature` method allows you to iterate over all the features of a
  Specification. When inside the `eachFeature` closure, you can access directly all members of the current feature
  (an instance of `FeatureInfo`). So, for example, to get the `Title` annotation of a feature, you can call
  `utils.specAnnotation( data, spock.lang.Title )`.

As the default template file shows, you can get statistics for the Specification easily with this code snippet:

```
<% def stats = utils.stats( data ) %>
Report statistics: $stats
```

The variable `stats` is a `Map` containing the following keys:

```
failures, errors, skipped, totalRuns, successRate, time
```

So, you can use it in your template like this:

```
Total number of runs:  ${stats.totalRuns}
Success rate........:  ${stats.successRate}
Number of failures..:  ${stats.failures}
Number of errors....:  ${stats.errors}
Number of ignored...:  ${stats.skipped}
Total time (ms).....:  ${stats.time}

Created on ${new Date()} by ${System.properties['user.name']}
```

#### Summary template

The summary template has access to a single variable called `data`, which is a Map containing all the available data for all Specifications that were run.

For example, after running two Specifications named `test.FirstSpec` and `test.SecondSpec`,
the `data` Map could look like this:

```groovy
[ 'test.FirstSpec': [ failures: 1, errors: 0, skipped: 0, totalRuns: 1, successRate: 0.0, time: 159],
  'test.SecondSpec': [ failures: 0, errors: 1, skipped: 0, totalRuns: 3, successRate: 0.6666666666666666, time: 8 ] ]
```

You can then iterate over each Spec's data as follows:

```
<% data.each { name, map ->
      def s = map.stats
 %>| $name | ${s.totalRuns} | ${s.failures} | ${s.errors} | ${s.skipped} | ${s.successRate} | ${s.time} |
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

Thank you.
