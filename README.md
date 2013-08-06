# Spock Reports Extension
*by Renato Athaydes*

## What it is

This project is a global extension for [Spock](https://code.google.com/p/spock/) to create test reports.
Currently, the only available report creator generates a **HTML report** for each Specification.

## Where to find demo reports

I am using [this CodePen](http://codepen.io/renatoathaydes/full/ihGgt) to design the HTML report. If you don't like the style, you can use your own css stylesheet (see the customization section below). I welcome feedback on how to improve the report looks!

## How to use it

> Unfortunately, for now this project is not available in Maven Central or any other repository,
> so you'll need to build it from source and install it in your local repo if you want to use dependency management, or just place the Jar in the classpath manually.


> To build and install this project, simply type `gradle install` from the root folder.

To enable this Spock extension, you only need to declare a dependency to it (if using Maven, Ivy, Gradle etc) or, in other words, add the jar to the classpath.

In Maven:

```xml
<dependency>
  <groupId>com.athaydes</groupId>
  <artifactId>spock-reports</artifactId>
  <version>1.0-SNAPSHOT</version>
  <scope>test</scope>
</dependency>
```

In Gradle:

```groovy
testCompile 'com.athaydes:spock-reports:1.0-SNAPSHOT'
```

The only dependencies of this project are on Groovy (version 2.0+) and Spock, but if you're using Spock (version 0.7-groovy-2.0+), you'll already have both!


## Customizing the reports

You can provide custom configuration in a properties file located at the following location (relative to the classpath):

`META-INF/services/com.athaydes.spockframework.report.IReportCreator.properties`

Here's the default properties file:

```properties
# Name of the implementation class of the report creator
# Currently supported classes are:
#   1. com.athaydes.spockframework.report.internal.HtmlReportCreator
com.athaydes.spockframework.report.IReportCreator=com.athaydes.spockframework.report.internal.HtmlReportCreator

# Set properties of the report creator
# For the HtmlReportCreator, the only settable property
# is the location of the css file to be used - relative to the classpath
com.athaydes.spockframework.report.internal.HtmlReportCreator.css=spock-feature-report.css

# Output directory (where the spock reports will be created) - relative to working directory
com.athaydes.spockframework.report.outputDir=build/spock-reports
```

Notice that the location of the css file is relative to the classpath!
That means that you have the freedom to place the css file in a separate jar, for example.

The output directory, on the other hand, is relative to the working directory.
For Maven projects which use the defaults, you might want to change it to `target/spock-reports`.
