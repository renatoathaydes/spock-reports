# Spock Reports Extension
*by Renato Athaydes*

## News

* Today, the 6th of August 2013, I am proud to release version 1.0 of this project! I have made sure to make it as stable
as possible so that you can start reading proper reports for your Spock specifications without worries.
You can find the jar in the `releases` directory! Download it, place it in your classpath and you're done!
By default, the reports will be saved in the directory `build/spock-reports`, but you can change that if you want,
just check below.
I hope you find this project as useful as I have (I actually developed this out of my own need for it!).
* I wrote a [blog post](https://software.athaydes.com/posts/writingspecificationsthatdoubleastestswithspock) about the motivation behind this project. Please check it out!

## What it is

This project is a global extension for [Spock](https://code.google.com/p/spock/) to create test (or, in Spock terms, Specifications) reports.
Currently, the only available report creator generates a **HTML report** for each Specification, as well as a summary of all Specifications that have been run (index.html).

## Where to find demo reports

I am using [CodePen](http://codepen.io) to design the HTML [feature report](http://cdpn.io/ihGgt), which contains detailed information about each Specification run by Spock, including the examples given (*Where* block) and their results, if any, and the [summary report](http://cdpn.io/mKckz), which summarizes the results of all Specification runs. Click on the links to see the reports used for testing.

If you don't like the styles, you can use your own css stylesheets (see the customization section below). I welcome feedback on how to improve the report looks!

## How to use it

> Unfortunately, for now this project is not available in Maven Central or any other repository,
> so you'll need to build it from source and install it in your local repo if you want to use dependency management, or just place the Jar (you can find it in the `releases` directory) in the classpath manually.


> To build and install this project, simply type `gradle install` from the root folder.

To enable this Spock extension, you only need to declare a dependency to it (if using Maven, Ivy, Gradle etc) or, in other words, add the jar to the classpath.

In Maven:

```xml
<dependency>
  <groupId>com.athaydes</groupId>
  <artifactId>spock-reports</artifactId>
  <version>1.0</version>
  <scope>test</scope>
</dependency>
```

In Gradle:

```groovy
testCompile 'com.athaydes:spock-reports:1.0'
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
# For the HtmlReportCreator, the only settable properties
# are the location of the css files to be used - relative to the classpath
com.athaydes.spockframework.report.internal.HtmlReportCreator.featureReportCss=spock-feature-report.css
com.athaydes.spockframework.report.internal.HtmlReportCreator.summaryReportCss=spock-summary-report.css

# Output directory (where the spock reports will be created) - relative to working directory
com.athaydes.spockframework.report.outputDir=build/spock-reports
```

Notice that the location of the css file is relative to the classpath!
That means that you have the freedom to place the css files in a separate jar, for example.

The output directory, on the other hand, is relative to the working directory.
For Maven projects which use the defaults, you might want to change it to `target/spock-reports`.
