package com.athaydes.spockframework.report.internal

import com.athaydes.spockframework.report.FakeTest
import com.athaydes.spockframework.report.ReportSpec
import com.athaydes.spockframework.report.SpockReportExtension
import com.athaydes.spockframework.report.UnrolledSpec
import com.athaydes.spockframework.report.VividFakeTest
import groovy.xml.MarkupBuilder
import org.junit.runner.Description
import org.junit.runner.notification.RunNotifier
import org.spockframework.runtime.Sputnik
import org.spockframework.runtime.model.SpecInfo
import spock.lang.Unroll

import java.nio.file.Paths

import static com.athaydes.spockframework.report.internal.TestHelper.minify

/**
 *
 * User: Renato
 */
@Unroll
class HtmlReportCreatorSpec extends ReportSpec {

    static final String UNKNOWN = 'Unknown'
    static final char DS = StringFormatHelper.ds

    def "A correct HTML report is generated for a #fakeTest.simpleName including different types of features"() {
        given:
        "The project build folder location is known"
        def buildDir = System.getProperty( 'project.buildDir', 'build' )

        and:
        "A known location where the report file will be saved that does not exist"
        def reportFile = Paths.get( buildDir, 'spock-reports', fakeTest.name + '.html' ).toFile()
        if ( reportFile.exists() ) {
            assert reportFile.delete()
        }

        and:
        "The expected HTML report (not counting time fields and errors)"
        String expectedHtml = expectedHtmlReport(fakeTest)

        and:
        "Method HtmlReportCreator.totalTime( SpecData ) is mocked out to fill time fields with known values"
        // using PredictableTimeResponse category to mock that

        and:
        "ProblemBlockWriter is mocked out to fill error blocks with known contents"
        // using PredictableProblems category to mock that

        and:
        "ToC Writer if mocked out to write something predictable"
        // using NoTocGenerated

        and:
        "String hashCodes produce predictable values"
        // using PredictableStringHashCode

        when:
        "A Specification containing different types of features is run by Spock"
        PredictableStringHashCode.code = 0
        use( ConfigOutputDir, PredictableTimeResponse, FakeKnowsWhenAndWhoRanTest, NoTocGenerated, PredictableStringHashCode ) {
            new Sputnik( fakeTest ).run( new RunNotifier() )
        }

        then:
        "A nice HTML report to have been generated under the build directory"
        reportFile.exists()

        and:
        "The contents are functionally the same as expected"
        minify( reportFile.text ) == minify( expectedHtml )

        where:
        fakeTest << [ FakeTest, VividFakeTest ]
    }

    def "The css file used should be loaded correctly from any file in the classpath"() {
        given:
        "A css file in the classpath with known contents"
        def reportFile = new File( this.class.getResource( "${this.class.simpleName}.class" ).toURI() )
        def cssFile = new File( reportFile.parentFile, 'my.css' ) << 'file-contents'

        when:
        "The css is set to the file location relative to the classpath"
        def reportCreator = new HtmlReportCreator( new HtmlReportAggregator() )
        reportCreator.css = this.class.package.name.replace( '.', '/' ) + '/my.css'

        then:
        "The css property of the report becomes the contents of the css file"
        reportCreator.css == 'file-contents'

        cleanup:
        cssFile?.delete()
    }

    def "The report aggregator should be given the required information for each spec visited"() {
        given:
        "A HtmlReportCreator with a mocked HtmlReportAggregator"
        def reportCreator = new HtmlReportCreator( Mock( HtmlReportAggregator ) )

        and:
        "The HtmlReportCreator has a known outputDir"
        reportCreator.outputDir = 'outputDir'

        and:
        "A mock report aggregator, a stubbed SpecData and an injected MarkupBuilder"
        def stubSpecData = Stub( SpecData )
        def stubInfo = Stub( SpecInfo )
        stubSpecData.info >> stubInfo
        stubInfo.description >> Description.createTestDescription( this.class, 'ignore' )
        def builder = new MarkupBuilder( Stub( Writer ) )

        when:
        "The report creator writes the summary report"
        reportCreator.writeSummary( builder, stubSpecData )

        then:
        "The stats shown in the summary report and the outputDir are passed on to the mock report aggregator"
        1 * reportCreator.reportAggregator.aggregateReport( _, _ )
    }

    def "The report aggregator should be passed on the summary css from the report creator"() {
        given:
        "A HtmlReportCreator"
        def reportCreator = new HtmlReportCreator( new HtmlReportAggregator() )

        and:
        "A css file location relative to the classpath"
        def cssPath = "spock-feature-report.css"

        when:
        "The summary report css is set on the HtmlReportCreator"
        reportCreator.summaryReportCss = cssPath

        then:
        "The report aggregator'css is set to the contents of the css file"
        reportCreator.reportAggregator.css == textOf( cssPath )

        cleanup:
        "Set css for summary report back to correct css"
        reportCreator.summaryReportCss = 'spock-summary-report.css'

    }

    def "A correct HTML report is generated for a spec where @Unrolled is added to the Specification itself"() {
        given:
        "The project build folder location is known"
        def buildDir = System.getProperty( 'project.buildDir', 'build' )

        and:
        "A known location where the report file will be saved that does not exist"
        def reportFile = Paths.get( buildDir, 'spock-reports',
                UnrolledSpec.class.name + '.html' ).toFile()
        if ( reportFile.exists() ) {
            assert reportFile.delete()
        }

        and:
        "The expected HTML report (not counting time fields and errors)"
        String expectedHtml = expectedUnrolledSpecHtmlReport()

        and:
        "Method HtmlReportCreator.totalTime( SpecData ) is mocked out to fill time fields with known values"
        // using PredictableTimeResponse category to mock that

        and:
        "ProblemBlockWriter is mocked out to fill error blocks with known contents"
        // using PredictableProblems category to mock that

        and:
        "ToC Writer if mocked out to write something predictable"
        // using NoTocGenerated

        and:
        "String hashCodes produce predictable values"
        // using PredictableStringHashCode

        when:
        "A Specification containing different types of features is run by Spock"
        PredictableStringHashCode.code = 0
        use( ConfigOutputDir, PredictableTimeResponse, FakeKnowsWhenAndWhoRanTest, NoTocGenerated, PredictableStringHashCode ) {
            new Sputnik( UnrolledSpec ).run( new RunNotifier() )
        }

        then:
        "A nice HTML report to have been generated under the build directory"
        reportFile.exists()

        and:
        "The contents are functionally the same as expected"
        minify( reportFile.text ) == minify( expectedHtml )
    }

    private String textOf( String cssPath ) {
        new File( this.class.classLoader.getResource( cssPath ).toURI() ).text
    }

    private String expectedHtmlReport(Class fakeTest) {
        def rawHtml = HtmlReportCreator.getResource( "${fakeTest.simpleName}Report.html" ).text
        def binding = fakeTestBinding(fakeTest)
        if( fakeTest == VividFakeTest ){
            binding += vividFakeTestBinding()
        }
        replacePlaceholdersInRawHtml( rawHtml, binding )
    }

    private Map<String, Object> fakeTestBinding( Class fakeTest ) {
        [
                classOnTest     : fakeTest.name,
                title           : 'This is just a Fake test to test spock-reports',
                narrative       : '\nAs a user\nI want foo\nSo that bar',
                style           : defaultStyle(),
                dateTestRan     : DATE_TEST_RAN,
                username        : TEST_USER_NAME,
                executedFeatures: 10,
                failures        : 3,
                errors          : 2,
                skipped         : 1,
                successRate     : "50${DS}0%",
                time            : UNKNOWN,
                projectUrl      : SpockReportExtension.PROJECT_URL
        ]
    }

    private Map<String, Object> vividFakeTestBinding() {
        [
                executedFeatures: 9,
                failures        : 3,
                errors          : 2,
                skipped         : 0,
                successRate     : "44${DS}44%"
        ]
    }


    private String expectedUnrolledSpecHtmlReport() {
        def rawHtml = HtmlReportCreator.getResource( 'UnrolledSpecReport.html' ).text
        def binding = [
                classOnTest     : UnrolledSpec.class.name,
                style           : defaultStyle(),
                dateTestRan     : DATE_TEST_RAN,
                username        : TEST_USER_NAME,
                executedFeatures: 5,
                failures        : 0,
                errors          : 0,
                skipped         : 0,
                successRate     : "100${DS}0%",
                time            : UNKNOWN,
                projectUrl      : SpockReportExtension.PROJECT_URL
        ]
        replacePlaceholdersInRawHtml( rawHtml, binding )
    }

    private String defaultStyle() {
        this.class.getResource( '/spock-feature-report.css' ).text
    }

    @Category( StringFormatHelper )
    class PredictableTimeResponse {
        String toTimeDuration( timeInMs ) { HtmlReportCreatorSpec.UNKNOWN }
    }

    @Category( KnowsWhenAndWhoRanTest )
    class FakeKnowsWhenAndWhoRanTest {
        String whenAndWhoRanTest( StringFormatHelper stringFormatter ) {
            "Created on ${ReportSpec.DATE_TEST_RAN} by ${ReportSpec.TEST_USER_NAME}"
        }
    }

    @Category( HtmlReportCreator )
    class NoTocGenerated {

        void writeFeatureToc( MarkupBuilder builder, SpecData data ) {
            builder.div( 'TOC' )
        }
    }

    @Category( HtmlReportCreator )
    class ConfigOutputDir {

        String getOutputDir() {
            System.getProperty( 'project.buildDir', 'build' ) + '/spock-reports'
        }
    }

    @Category( String )
    class PredictableStringHashCode {
        static code = 0

        int hashCode() {
            code++
        }
    }

}
