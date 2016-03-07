package com.athaydes.spockframework.report.internal

import com.athaydes.spockframework.report.ReportSpec
import com.athaydes.spockframework.report.SpockReportExtension
import groovy.xml.MarkupBuilder
import org.junit.runner.Description
import org.spockframework.runtime.model.SpecInfo

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
        def stats = [ failures: 1, errors: 0, skipped: 2, totalRuns: 5, successRate: 0.25, time: 0 ]

        and:
        "An output directory"
        def outputDir = "build/${this.class.simpleName}"

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
        minify( reportFile.text ) == minify( singleTestSummaryExpectedHtml() )
    }

    def """When several specs data are provided to the HtmlReportAggregator it
           should create a report aggregating all of the individual spec reports"""() {
        given:
        "Several specs"
        def allSpecs = [
                'Spec1'      : [ failures: 0, errors: 0, skipped: 2, totalRuns: 5, successRate: 0.1, time: 1000 ],      // 1
                'a.Spec2'    : [ failures: 0, errors: 1, skipped: 3, totalRuns: 6, successRate: 0.2, time: 2000 ],    // 2
                'a.Spec3'    : [ failures: 3, errors: 2, skipped: 4, totalRuns: 7, successRate: 0.3, time: 3000 ],    // 3
                'a.b.c.Spec4': [ failures: 4, errors: 3, skipped: 5, totalRuns: 8, successRate: 0.4, time: 4000 ],// 4
                'b.c.Spec6'  : [ failures: 6, errors: 5, skipped: 7, totalRuns: 10, successRate: 0.6, time: 5000 ], // 6
                'a.b.c.Spec5': [ failures: 5, errors: 4, skipped: 6, totalRuns: 9, successRate: 0.5, time: 6000 ],// 5
                'c.d.Spec6'  : [ failures: 7, errors: 6, skipped: 8, totalRuns: 11, successRate: 0.7, time: 7000 ]  // 7
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
                    getDescription() >> Description.createTestDescription( name , name)
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
        minify( reportFile.text ) == minify( testSummaryExpectedHtml() )
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

    private String singleTestSummaryExpectedHtml() {
        def rawHtml = this.class.getResource( 'SingleTestSummaryReport.html' ).text
        def binding = [ dateTestRan: DATE_TEST_RAN,
                        username   : TEST_USER_NAME, ]
        replacePlaceholdersInRawHtml( rawHtml, binding )
    }

}
