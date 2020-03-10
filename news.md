## News

> Most recent first

* `10th of March 2020`

This release (1.7.0) does not contain code changes. It is only a dependency upgrade release that should bring
support for builds running Java 9+ (tested up to Java 12).

Java 8 can still be used, but version spock-reports version 1.7.1 should be used!

This is expected to be the last 1.x release, as version 2.0 is already in the works!
I intend to base spock-reports 2.x on Groovy 3 and Spock 2.x.

The 1.x branch will still be maintained, but will only receive important bug fixes (no new features).

* `04th of January 2020`

spock-reports version 1.6.3 released with several bug fixes, including some important changes on how feature counts
are calculated.

* `10th of April 2019`

Version 1.6.2 of spock-reports released! This is a bug-fix release, but it fixes some things that may affect a lot
of users, so an upgrade is highly recommended.

* `23rd of September 2018`

1.6.1 version allows template report authors to access time information for each feature run iteration. The default
markdown template was updated to demonstrate how that information can be accessed.

This release also contains small bug fixes and it allows changing the location of the aggregated JSON report.

* `30th of May 2018`

Small release fixing small issues and adding a new feature: the specification's titles, by default, will now be shown
in the HTML summary report along with the class' name. This can be configured to show only class name or only title
(when available) if desired. Details in the README page.

* `27th of March 2018`

Quite a lot of improvements in this release, including a couple of bug fixes and some small, but nice new features.

The main new feature is support for Spock's `SpockConfig.groovy` file, which can now be used to configure spock-reports.

So, if you already configure other Spock extensions via the `SpockConfig.groovy` file, now there's no need to create a
separate file just for spock-reports.

Here's a simple example configuration file you could use:

```
// configure the @Issue extension
report {
    issueUrlPrefix = 'http://myserver.com/issues/'
}

// configure spock-reports
spockReports {
    set 'com.athaydes.spockframework.report.showCodeBlocks': true
    set 'com.athaydes.spockframework.report.outputDir': 'target/spock-reports'
}
```

All properties are supported, of course! See more info in the [README](README.md) page.

* `26th of September 2017`

Version 1.3.2 of spock-reports released.

This release brings two big improvements:

- system properties can now be used to override any property supported in configuration files.
- vivid reports now highlight which line of code caused a test to fail.

These improvements are so great that I decided to write a [blog post](https://sites.google.com/a/athaydes.com/renato-athaydes/posts/spock-reports-greattestreportsthatyouactuallywanttoread)
about them! 

* `24th of September 2017`

@tilmanginzel published an extension of spock-reports that works with [Geb](http://gebish.org/) to add support for
screenshots in web test reports!

Check out [geb-spock-reports](https://github.com/AOEpeople/geb-spock-reports) if you use Spock and Geb! 

* `18th of May 2017`

Minor release fixing a bug in the new, optional vivid reports feature.

* `21st of April 2017`

Lots of improvements in this major release (version 1.3.0), including a long overdue overhaul of the 
HTML report's CSS declarations that makes the HTML reports just a little bit more modern.

It also has a great new feature called vivid reports. It lets you show the test's source code in the report, just below
the block description! It is not turned on by default (at least for now), see instructions in the README page for how
to turn that one if you would like to add code to your reports (and why not!?). 

Special thanks to @hexmind in this release, for helping with the vivid reports!

* `02nd of October 2016`

Version 1.2.13 has been released!

This is a minor release with a few minor bug fixes, many of which are related to Specifications that fail to initialize.
Now, a report will be created for every Specification that is attempted to run, even if failing catastrophically on
initialization, with a useful error message shown on the report.

* `09th of June 2016`

Version 1.2.12 has been released!

Besides a few bug fixes and modest performance improvements, this release brings much better handling of failures
during Specification setup. All features of the Specification will be shown as FAILED, and the error will be displayed 
in the report under the first feature (to show the stacktrace, just enable that option in the config file).

This version also ensures that the report will show features in their execution order.
This was not necessarily the case previously.
Thanks to @tilmanginzel for the contribution to this feature. 

* `19th of May 2016`

This is a small bug fix release.
Importantly, though, it fixes the basic template report summary which had stopped working in version `1.2.10`.

* `08th of March 2016`

1.2.10 release - this release brings an improved aggregated report in the JSON format.

Previous versions of spock-reports already generated a file called .aggregated_data in the reports directory.
This file was a JSON representation of all specifications run by Spock, and was used to keep track
of tests running in separate processes, so that the index.html file could contain all tests, not only the ones
running in the last process to terminate.

This file, however, turned out to be useful when you have multi-projects and end up with multiple index.html reports
for each project... because this file is in the json format and is so easy to parse, external tools can use these
files to aggregate the summaries of all projects into a single, final report, much more easily.

In this release, the JSON report aggregation file was improved and renamed to `aggregated_report.json`
so that it is more visible and can be freely used by external tools.

* `14th of February 2016`

1.2.9 release - this is the first release to be published on Maven Central!

Many users complained about restrictions regarding Maven repositories in their working place, so hopefully this
will help a lot of users to start getting proper reports out of their Spock specifications.

Another worthy new feature in this release is the option to not inline CSS in HTML files, avoiding CSP violations
 (see issue #54).

* `6th of January 2016`

1.2.8 release - a new feature and a few fixes are included in this release.

Also, the logging framework has been changed from JUL to Slf4J due to several complaints about JUL making it hard to
configure the logging behaviour of spock-reports. Now, you can easily choose a logger implementation that is compatible
with slf4j, such as Log4j or Logback, and configure it to enable as much logging as you wish from spock-reports!

The new feature: it is now possible to, optionally, include the stack-trace of a Throwable that caused a test failure
in HTML reports. This should be useful for developers to track down bugs without having to chase stacktraces using
other sources.

* `15th of August 2015`

1.2.7 release - a really quick release this time! That's because the previous release, 1.2.6, unfortunately missed
adding support for documentation annotations (@Ignore, @Issue, @See, @Title, @Narrative). Support for all of these 
annotations have been added in this release (the release notes have been amended to reflect this).

As usual, this release also includes some tiny bug fixes... and template reports are now easier to write.

* `12th of August 2015`

1.2.6 release - this release brings support for **parallel builds**. Until now, individual specification reports would
be created just fine, but the aggregated report would only contain the specifications run by the last Thread of a
parallel build. After this release, that is no longer the case!

Other minor improvements were also included in this release, check the release notes for details.

**BREAKING CHANGE**: for users of template reports, a necessary breaking change had to made: in Specification report
templates, to iterate over features you must now call method `eachFeature` instead of `forEach`. This was necessary
 because the old name conflicted with a native method when using Java 8. Sorry for the inconvenience.

* `8th of March 2015`

1.2.5 release - added support for [Spock 1.0](http://spockframework.github.io/spock/docs/1.0/release_notes.html),
which has been released, finally!!
Also, but not less important, now spock-reports has support for templates! This lets you make your report look
like whatever you want!

* `14th of February 2015`

1.2.4 released - some nice new features included, such as Table of Contents.

* `08th of November 2014`

1.2.3 released - downgraded minimum Groovy version required to 2.0.8 (benefits some Grails users).

* `03rd of November 2014`

Small feature release (1.2.2): added support for @Unroll-ing variables not only in Spec names,
but also in block texts (given, when, then...)

* `01st of November 2014`

Bug fix release, 1.2.1. Fixed problems with bad-behaving Specification setup methods and
removed usage of some Groovy features to allow running spock-reports with any recent JDK 7 or 8 version.

* `3rd of August 2014`

Release of version 1.2! This release includes full support for @Unroll,
amongst other small improvements.

* `3rd of July 2014`

After a long delay, I have finally published the 1.1 release on a public repo, thanks to @JayStGelais
contribution. So now ``spock-reports`` is available on Bintray's [JCenter](http://jcenter.bintray.com/)!

* `14th of September 2013`

Release of version 1.1 with some minor bug fixes and improvements in the information shown in reports.
Please check the release notes in the file `releases/Release_Notes.txt`.

* `6th of August 2013`

Today, I am proud to release version 1.0 of this project! I have made sure to make it as stable
as possible so that you can start reading proper reports for your Spock specifications without worries.
You can find the jar in the `releases` directory! Download it, place it in your classpath and you're done!
By default, the reports will be saved in the directory `build/spock-reports`, but you can change that if you want,
just check below.
I hope you find this project as useful as I have (I actually developed this out of my own need for it!).


I wrote a [blog post](http://software.athaydes.com/posts/writingspecificationsthatdoubleastestswithspock) about the motivation behind this project. Please check it out!
