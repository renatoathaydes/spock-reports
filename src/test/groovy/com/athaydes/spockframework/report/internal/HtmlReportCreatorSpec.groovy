package com.athaydes.spockframework.report.internal

import com.athaydes.spockframework.report.*
import com.athaydes.spockframework.report.engine.CanRunSpockSpecs
import com.athaydes.spockframework.report.util.Hasher
import groovy.xml.MarkupBuilder
import org.spockframework.runtime.model.SpecInfo
import spock.lang.Unroll

import java.nio.file.Paths

import static com.athaydes.spockframework.report.internal.TestHelper.assertVerySimilar
import static com.athaydes.spockframework.report.internal.TestHelper.minify

@Unroll
class HtmlReportCreatorSpec extends ReportSpec
        implements CanRunSpockSpecs {

    static final String UNKNOWN = 'Unknown'
    static final char DS = StringFormatHelper.ds

    static final Map fakeTestBinding = [
            title           : 'This is just a Fake test to test spock-reports',
            narrative       : '\nAs a user\nI want foo\nSo that bar',
            executedFeatures: 10,
            passed          : 5,
            failures        : 3,
            errors          : 2,
            skipped         : 2,
            successRate     : "50${DS}0%"
    ]

    static final Map vividFakeTestBinding = [
            narrative       : '\nAs a developer\nI want to see my code',
            executedFeatures: 9,
            passed          : 4,
            failures        : 3,
            errors          : 2,
            skipped         : 0,
            successRate     : "44${DS}44%"
    ]

    static final Map fullyIgnoredSpecBinding = [
            executedFeatures: 0,
            passed          : 0,
            failures        : 0,
            errors          : 0,
            skipped         : 2,
            successRate     : "100%"
    ]

    @Unroll
    def "A correct HTML report is generated for a #specification.simpleName including different types of features"() {
        given:
        "The project build folder location is known"
        def buildDir = System.getProperty( 'project.buildDir', 'build' )

        and:
        "A known location where the report file will be saved that does not exist"
        def reportFile = Paths.get( buildDir, 'spock-reports', specification.name + '.html' ).toFile()
        if ( reportFile.exists() ) {
            assert reportFile.delete()
        }

        and:
        "The expected HTML report (not counting time fields and errors)"
        String expectedHtml = expectedHtmlReport( specification, reportBinding )

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
        use( configShowCodeBlocks, ConfigOutputDir, PredictableTimeResponse, FakeKnowsWhenAndWhoRanTest,
                NoTocGenerated, PredictableStringHashCode ) {
            runSpec( specification )
        }

        then:
        "A nice HTML report to have been generated under the build directory"
        reportFile.exists()

        and:
        "The contents are functionally the same as expected"
        def minifiedActualReport = minify( reportFile.text )
        def minifiedExpectedReport = minify( expectedHtml )
        assertVerySimilar( minifiedActualReport, minifiedExpectedReport )

        where:
        specification | configShowCodeBlocks   | reportBinding
        FakeTest      | ShowCodeBlocksDisabled | fakeTestBinding
        VividFakeTest | ShowCodeBlocksEnabled  | vividFakeTestBinding
        // FIXME https://github.com/renatoathaydes/spock-reports/issues/242
        // FullyIgnoredSpec | ShowCodeBlocksDisabled | fullyIgnoredSpecBinding
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
        def stubInfo = Stub( SpecInfo )
        def stubSpecData = new SpecData( stubInfo )
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
        String expectedHtml = expectedHtmlReport( UnrolledSpec, reportBinding )

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
        use( ConfigOutputDir, PredictableTimeResponse,
                FakeKnowsWhenAndWhoRanTest, NoTocGenerated,
                PredictableStringHashCode, ShowCodeBlocksDisabled ) {
            runSpec( UnrolledSpec )
        }

        then:
        "A nice HTML report to have been generated under the build directory"
        reportFile.exists()

        and:
        "The contents are functionally the same as expected"
        def minifiedActualReport = minify( reportFile.text )
        def minifiedExpectedReport = minify( expectedHtml )
        assertVerySimilar( minifiedActualReport, minifiedExpectedReport )

        where:
        reportBinding = [
                executedFeatures: 5,
                passed          : 5,
                failures        : 0,
                errors          : 0,
                skipped         : 0,
                successRate     : "100${DS}0%"
        ]
    }

    def "A correct HTML report is generated for a Specification including extra information added dynamically"() {
        given:
        "The project build folder location is known"
        def buildDir = System.getProperty( 'project.buildDir', 'build' )

        and:
        "A Specification containing features that add extra information to the report"
        def specification = SpecIncludingExtraInfo

        and:
        "Report binding data"
        def reportBinding = [
                executedFeatures: 6,
                passed          : 6,
                failures        : 0,
                errors          : 0,
                skipped         : 0,
                successRate     : "100${DS}0%"
        ]

        and:
        "A known location where the report file will be saved that does not exist"
        def reportFile = Paths.get( buildDir, 'spock-reports', specification.name + '.html' ).toFile()
        if ( reportFile.exists() ) {
            assert reportFile.delete()
        }

        and:
        "The expected HTML report (not counting time fields and errors)"
        String expectedHtml = expectedHtmlReport( specification, reportBinding )

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
            runSpec( specification )
        }

        then:
        "A nice HTML report to have been generated under the build directory"
        reportFile.exists()

        and:
        "The contents are functionally the same as expected"
        def minifiedActualReport = minify( reportFile.text )
        def minifiedExpectedReport = minify( expectedHtml )
        assertVerySimilar( minifiedActualReport, minifiedExpectedReport )
    }

    private String textOf( String cssPath ) {
        new File( this.class.classLoader.getResource( cssPath ).toURI() ).text
    }

    private String expectedHtmlReport( Class specification, Map reportBinding ) {
        def rawHtml = HtmlReportCreator.getResource( "${specification.simpleName}Report.html" ).text
        def binding = defaultBinding( specification ) + reportBinding
        replacePlaceholdersInRawHtml( rawHtml, binding )
    }

    private Map<String, Object> defaultBinding( Class specification ) {
        [
                classOnTest: specification.name,
                style      : defaultStyle(),
                dateTestRan: DATE_TEST_RAN,
                username   : TEST_USER_NAME,
                time       : UNKNOWN,
                projectUrl : SpockReportExtension.PROJECT_URL
        ]
    }

    private String defaultStyle() {
        this.class.getResource( '/spock-feature-report.css' ).text
    }

    @Category( StringFormatHelper )
    static class PredictableTimeResponse {
        String toTimeDuration( timeInMs ) { HtmlReportCreatorSpec.UNKNOWN }
    }

    @Category( KnowsWhenAndWhoRanTest )
    static class FakeKnowsWhenAndWhoRanTest {
        String whenAndWhoRanTest( StringFormatHelper stringFormatter ) {
            "Created on ${ReportSpec.DATE_TEST_RAN} by ${ReportSpec.TEST_USER_NAME}"
        }
    }

    @Category( HtmlReportCreator )
    static class NoTocGenerated {

        void writeFeatureToc( MarkupBuilder builder, SpecData data ) {
            builder.div( 'TOC' )
        }
    }

    @Category( HtmlReportCreator )
    static class ConfigOutputDir {

        String getOutputDir() {
            System.getProperty( 'project.buildDir', 'build' ) + '/spock-reports'
        }
    }

    @Category( Hasher )
    static class PredictableStringHashCode {
        static int code = 0

        String hash( String text ) {
            ( code++ ).toString()
        }
    }

    @Category( SpockReportExtension )
    static class ShowCodeBlocksEnabled {
        static final htmlReportCreator = new HtmlReportCreator(
                outputDir: System.getProperty( 'project.buildDir', 'build' ) + '/spock-reports',
                featureReportCss: 'spock-feature-report.css',
                summaryReportCss: 'spock-summary-report.css',
                showCodeBlocks: true )

        SpecInfoListener createListener() {
            new SpecInfoListener( htmlReportCreator )
        }
    }

    @Category( SpockReportExtension )
    static class ShowCodeBlocksDisabled {
        static final htmlReportCreator = new HtmlReportCreator(
                outputDir: System.getProperty( 'project.buildDir', 'build' ) + '/spock-reports',
                featureReportCss: 'spock-feature-report.css',
                summaryReportCss: 'spock-summary-report.css',
                showCodeBlocks: false )

        SpecInfoListener createListener() {
            new SpecInfoListener( htmlReportCreator )
        }
    }

}
