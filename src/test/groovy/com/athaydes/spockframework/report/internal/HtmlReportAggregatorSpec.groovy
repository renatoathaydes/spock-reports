package com.athaydes.spockframework.report.internal

import com.athaydes.spockframework.report.ReportSpec
import com.athaydes.spockframework.report.SpockReportExtension
import groovy.xml.MarkupBuilder
import org.junit.runner.Description
import org.spockframework.runtime.model.FeatureInfo
import org.spockframework.runtime.model.SpecInfo

import java.util.concurrent.TimeUnit

import static com.athaydes.spockframework.report.internal.TestHelper.assertVerySimilar
import static com.athaydes.spockframework.report.internal.TestHelper.minify

/**
 *
 * User: Renato
 */
class HtmlReportAggregatorSpec extends ReportSpec {

    def """When a single spec data is provided to the HtmlReportAggregator it
           should create a report with data from the single spec"""() {
        given:
        "A single spec stats"
        def stats = [ failures: 1, errors: 0, skipped: 2, totalRuns: 3, totalFeatures: 5, successRate: 0.25, time: 0 ]

        and:
        "A clean output directory"
        def outputDir = "build/${this.class.simpleName}"
        def outputDirFile = new File( outputDir )
        if ( outputDirFile.directory ) {
            assert outputDirFile.deleteDir()
        }

        and:
        "A HtmlReportAggregator with mocked out dependencies and writeFooter() method"
        def aggregator = new HtmlReportAggregator( outputDirectory: outputDir )
        aggregator.whenAndWho = mockKnowsWhenAndWhoRanTest()

        def mockStringFormatter = Stub( StringFormatHelper )
        mockStringFormatter.toPercentage( _ ) >>> [ '25.0%', '25%' ]
        mockStringFormatter.toTimeDuration( _ ) >>> [ '1.0 second', '1 sec' ]

        aggregator.stringFormatter = mockStringFormatter

        aggregator.metaClass.writeFooter = { MarkupBuilder builder ->
            builder.div( 'class': 'footer', 'The footer' )
        }

        when:
        "The spec data is provided to the HtmlReportAggregator"
        def specDataStub = Stub( SpecData ) {
            getInfo() >> Stub( SpecInfo ) {
                getDescription() >> Description.createTestDescription( 'Spec1', 'Spec1' )
            }
        }
        aggregator.aggregateReport( specDataStub, stats )
        aggregator.writeOut()
        def reportFile = new File( outputDir, 'index.html' )

        then:
        "An aggregated report called index.html should be created in the outputDir"
        reportFile.exists()

        and:
        "The contents are functionally the same as expected"
        def minifiedActualReport = minify( reportFile.text )
        def minifiedExpectedReport = minify( singleTestSummaryExpectedHtml() )
        assertVerySimilar( minifiedActualReport, minifiedExpectedReport )
    }

    def """When a single spec data is provided to the HtmlReportAggregator it
           should create a report with data from the single spec including project name and version"""() {
        given:
        "A single spec stats"
        def stats = [ failures: 1, errors: 0, skipped: 2, totalRuns: 3, totalFeatures: 5, successRate: 0.25, time: 0 ]

        and:
        "A clean output directory"
        def outputDir = "build/${this.class.simpleName}"
        def outputDirFile = new File( outputDir )
        if ( outputDirFile.directory ) {
            assert outputDirFile.deleteDir()
        }

        and:
        "A HtmlReportAggregator with mocked out dependencies and writeFooter() method"
        def aggregator = new HtmlReportAggregator( outputDirectory: outputDir )
        aggregator.whenAndWho = mockKnowsWhenAndWhoRanTest()

        def mockStringFormatter = Stub( StringFormatHelper )
        mockStringFormatter.toPercentage( _ ) >>> [ '25.0%', '25%' ]
        mockStringFormatter.toTimeDuration( _ ) >>> [ '1.0 second', '1 sec' ]

        aggregator.stringFormatter = mockStringFormatter

        aggregator.metaClass.writeFooter = { MarkupBuilder builder ->
            builder.div( 'class': 'footer', 'The footer' )
        }

        and:
        "The HtmlReportAggregator receives a project name and version"
        aggregator.projectName = 'Super Project'
        aggregator.projectVersion = '1.8u112'

        when:
        "The spec data is provided to the HtmlReportAggregator"
        def specDataStub = Stub( SpecData ) {
            getInfo() >> Stub( SpecInfo ) {
                getDescription() >> Description.createTestDescription( 'Spec1', 'Spec1' )
            }
        }
        aggregator.aggregateReport( specDataStub, stats )
        aggregator.writeOut()
        def reportFile = new File( outputDir, 'index.html' )

        then:
        "An aggregated report called index.html should be created in the outputDir"
        reportFile.exists()

        and:
        "The contents are functionally the same as expected"
        def expectedProjectHeader = """
        <div class='project-header'>
          <span class='project-name'>Project: ${aggregator.projectName}</span>
          <span class='project-version'>Version: ${aggregator.projectVersion}</span>
        </div>"""


        def minifiedActualReport = minify( reportFile.text )
        def minifiedExpectedReport = minify( singleTestSummaryExpectedHtml( expectedProjectHeader ) )
        assertVerySimilar( minifiedActualReport, minifiedExpectedReport )
    }

    def """When several specs data are provided to the HtmlReportAggregator it
           should create a report aggregating all of the individual spec reports"""() {
        given:
        "Several specs"
        def allSpecs = [
                'Spec1'      : [ failures: 0, errors: 0, skipped: 2, totalFeatures: 5, successRate: 0.1, time: 1000 ],
                'a.Spec2'    : [ failures: 0, errors: 1, skipped: 3, totalFeatures: 6, successRate: 0.2, time: 2000 ],
                'a.Spec3'    : [ failures: 3, errors: 2, skipped: 4, totalFeatures: 7, successRate: 0.3, time: 3000 ],
                'a.b.c.Spec4': [ failures: 4, errors: 3, skipped: 5, totalFeatures: 8, successRate: 0.4, time: 4000 ],
                'b.c.Spec6'  : [ failures: 6, errors: 5, skipped: 7, totalFeatures: 10, successRate: 0.6, time: 5000 ],
                'a.b.c.Spec5': [ failures: 5, errors: 4, skipped: 6, totalFeatures: 9, successRate: 0.5, time: 6000 ],
                'c.d.Spec6'  : [ failures: 7, errors: 6, skipped: 8, totalFeatures: 11, successRate: 0.7, time: 7000 ]
        ]

        and:
        "An output directory"
        def outputDir = "build/${this.class.simpleName}"

        and:
        "A HtmlReportAggregator with mocked dependencies and the test css style"
        def aggregator = new HtmlReportAggregator( css: 'spock-feature-report.css', outputDirectory: outputDir )
        aggregator.whenAndWho = mockKnowsWhenAndWhoRanTest()

        when:
        "The specs data is provided to the HtmlReportAggregator"
        allSpecs.each { String name, Map stats ->
            def specDataStub = Stub( SpecData ) {
                getInfo() >> Stub( SpecInfo ) {
                    getDescription() >> Description.createTestDescription( name, name )
                }
            }
            aggregator.aggregateReport( specDataStub, stats )
        }
        aggregator.writeOut()
        def reportFile = new File( outputDir, 'index.html' )

        then:
        "An aggregated report called index.html should be created in the outputDir"
        reportFile.exists()

        and:
        "The contents are functionally the same as expected"
        def minifiedActualReport = minify( reportFile.text )
        def minifiedExpectedReport = minify( testSummaryExpectedHtml() )
        assertVerySimilar( minifiedActualReport, minifiedExpectedReport )
    }

    def "Can aggregate reports data into a Map for persistence"() {
        given: 'A HtmlReportAggregator'
        def aggregator = new HtmlReportAggregator( css: 'spock-feature-report.css', outputDirectory: 'out' )

        and: 'Some realistic, mocked out specData'
        def data = Stub( SpecData ) {
            getInfo() >> Stub( SpecInfo ) {
                getDescription() >> Description.createTestDescription( 'myClass', 'myClass' )
                getAllFeaturesInExecutionOrder() >> [
                        Stub( FeatureInfo ) {
                            isSkipped() >> false
                            getDescription() >> Description.createTestDescription( 'myClass', 'myClass' )
                            getName() >> 'cFeature'
                        },
                        Stub( FeatureInfo ) {
                            isSkipped() >> true
                            getDescription() >> Description.createTestDescription( 'myClass', 'myClass' )
                            getName() >> 'aFeature'
                        },
                        Stub( FeatureInfo ) {
                            isSkipped() >> false
                            getDescription() >> Description.createTestDescription( 'myClass', 'myClass' )
                            getName() >> 'bFeature'
                        }
                ]
            }
        }

        and: 'Some statistics'
        def stats = [ x: 1, y: 2 ]

        when: 'The report aggregator aggregates the data and stats'
        aggregator.aggregateReport( data, stats )

        then: 'The resulting Map should contain an entry for the specData class'
        aggregator.aggregatedData.containsKey( 'myClass' )
        aggregator.aggregatedData.size() == 1

        Map json = aggregator.aggregatedData.myClass

        and: 'The Map within that key contains the correct information regarding the spec'
        json.stats == stats
        json.ignoredFeatures == [ 'aFeature' ]
        json.executedFeatures == [ 'bFeature', 'cFeature' ]
    }

    def "Must be able to lock the report file properly across JVMs"() {
        given: 'A report file'
        final file = File.createTempFile( getClass().name, 'txt' )
        file.write( '1' )

        when: 'the file is written to by several Threads in another JVMs, protected with a lock'
        final threadCount = 20
        def forkedJvmProcess = executeMainInForkedProcess( SimulatedReportWriter,
                file.absolutePath, threadCount.toString() )

        and: 'the file is written to simultaneously in this JVM, in the same manner'
        sleep 200 // wait for the other JVM startup-time
        def localJvmLatch = SimulatedReportWriter.write( file, threadCount )

        then: 'both writers should finish within a reasonable timeout'
        localJvmLatch.await( 10, TimeUnit.SECONDS )
        forkedJvmProcess.waitFor( 15, TimeUnit.SECONDS )

        and: 'the forked JVM should finish successfully'
        // in case of error, this will print the errorStream
        forkedJvmProcess.exitValue() == 0 || forkedJvmProcess.errorStream.text == 'FAILED'

        and: 'The contents of the file must be the predicted value if the lock works'
        file.text == ( 1..41 ).join( ' ' )
    }

    def "Should be able to write spec name or title according to chosen settings"() {
        given: 'A HtmlReportAggregator using a specific summary option of #summarySetting'
        def aggregator = new HtmlReportAggregator(
                specSummaryNameOption: SpecSummaryNameOption.valueOf( summarySetting.toUpperCase() ) )

        and: 'A MarkupBuilder'
        def writer = new StringWriter()
        def builder = new MarkupBuilder( writer )

        when: 'The details of the data is written with the builder'
        aggregator.writeSpecSummary( builder, stats, specName, specTitle )

        then: 'The data should be written according to chosen settings'

        def minifiedExpectedSummary = minify( expectedSummary )
        def minifiedActualSummary = minify( writer.toString() )
        assertVerySimilar( minifiedExpectedSummary, minifiedActualSummary )

        where:
        stats                    | specName | specTitle | summarySetting | expectedSummary
        [ failures     : 1,
          errors       : 0,
          skipped      : 2,
          totalFeatures: 5,
          successRate  : 0.25,
          time         : 0 ]     |
                'abc.SpecA'                 |
                'Spec A'                                |
                'class_name_and_title'                                   |
                "<tr class='failure'><td><a href='abc.SpecA.html'>abc.SpecA</a><div class='spec-title'>Spec A</div>" +
                "</td><td>5</td><td>1</td><td>0</td><td>2</td><td>25.0%</td><td>0</td></tr>"

        [ failures     : 1,
          errors       : 0,
          skipped      : 2,
          totalFeatures: 5,
          successRate  : 0.25,
          time         : 0 ]     |
                'abc.SpecA'                 |
                ''                                      |
                'class_name_and_title'                                   |
                "<tr class='failure'><td><a href='abc.SpecA.html'>abc.SpecA</a>" +
                "</td><td>5</td><td>1</td><td>0</td><td>2</td><td>25.0%</td><td>0</td></tr>"

        [ failures     : 2,
          errors       : 4,
          skipped      : 1,
          totalFeatures: 7,
          successRate  : 0.33,
          time         : 1_000 ] |
                'abc.SpecA'                 |
                'Spec A'                                |
                'title'                                                  |
                "<tr class='failure error'><td><a href='abc.SpecA.html'><div class='spec-title'>Spec A</div></a>" +
                "</td><td>7</td><td>2</td><td>4</td><td>1</td><td>33.0%</td><td>1.000 seconds</td></tr>"

        [ failures     : 2,
          errors       : 4,
          skipped      : 1,
          totalFeatures: 7,
          successRate  : 0.33,
          time         : 1_000 ] |
                'abc.SpecA'                 |
                ''                                      |
                'title'                                                  |
                "<tr class='failure error'><td><a href='abc.SpecA.html'>abc.SpecA</a>" +
                "</td><td>7</td><td>2</td><td>4</td><td>1</td><td>33.0%</td><td>1.000 seconds</td></tr>"

        [ failures     : 2,
          errors       : 4,
          skipped      : 1,
          totalFeatures: 7,
          successRate  : 0.33,
          time         : 1_000 ] |
                'abc.SpecA'                 |
                'Spec A'                                |
                'class_name'                                             |
                "<tr class='failure error'><td><a href='abc.SpecA.html'>abc.SpecA</a>" +
                "</td><td>7</td><td>2</td><td>4</td><td>1</td><td>33.0%</td><td>1.000 seconds</td></tr>"
    }

    private static Process executeMainInForkedProcess( Class mainClass, String... args ) {
        def java = System.getProperty( 'java.home' )
        def cp = System.getProperty( 'java.class.path' )
        [ "$java/bin/java", '-cp', cp, mainClass.name, *args ].execute()
    }

    private String testSummaryExpectedHtml() {
        def sf = new StringFormatHelper()
        def rawHtml = this.class.getResource( 'TestSummaryReport.html' ).text
        def binding = [
                style       : defaultStyle(),
                dateTestRan : DATE_TEST_RAN,
                username    : TEST_USER_NAME,
                total       : 7,
                passed      : 1,
                failed      : 6,
                fFails      : 25,
                fErrors     : 21,
                successRate : sf.toPercentage( 1 / 7 ),
                time        : sf.toTimeDuration( 28_000 ),
                projectUrl  : SpockReportExtension.PROJECT_URL,
                successRate1: sf.toPercentage( 0.1 ),
                duration1   : sf.toTimeDuration( 1000 ),
                successRate2: sf.toPercentage( 0.2 ),
                duration2   : sf.toTimeDuration( 2000 ),
                successRate3: sf.toPercentage( 0.3 ),
                duration3   : sf.toTimeDuration( 3000 ),
                successRate4: sf.toPercentage( 0.4 ),
                duration4   : sf.toTimeDuration( 4000 ),
                successRate5: sf.toPercentage( 0.5 ),
                duration5   : sf.toTimeDuration( 6000 ),
                successRate6: sf.toPercentage( 0.6 ),
                duration6   : sf.toTimeDuration( 5000 ),
                successRate7: sf.toPercentage( 0.7 ),
                duration7   : sf.toTimeDuration( 7000 )
        ]
        replacePlaceholdersInRawHtml( rawHtml, binding )
    }

    private String defaultStyle() {
        this.class.getResource( '/spock-feature-report.css' ).text
    }

    private String singleTestSummaryExpectedHtml( projectHeader = '' ) {
        def rawHtml = this.class.getResource( 'SingleTestSummaryReport.html' ).text
        def binding = [ dateTestRan  : DATE_TEST_RAN,
                        username     : TEST_USER_NAME,
                        projectHeader: projectHeader ]
        replacePlaceholdersInRawHtml( rawHtml, binding )
    }

}
