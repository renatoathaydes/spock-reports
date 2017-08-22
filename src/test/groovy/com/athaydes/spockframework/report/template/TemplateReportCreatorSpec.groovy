package com.athaydes.spockframework.report.template

import com.athaydes.spockframework.report.FakeTest
import com.athaydes.spockframework.report.SpecInfoListener
import com.athaydes.spockframework.report.SpockReportExtension
import com.athaydes.spockframework.report.VividFakeTest
import com.athaydes.spockframework.report.internal.HtmlReportCreatorSpec
import com.athaydes.spockframework.report.internal.StringFormatHelper
import org.junit.runner.notification.RunNotifier
import org.spockframework.runtime.Sputnik
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Paths

@Unroll
class TemplateReportCreatorSpec extends Specification {

    def "A correct Template report is generated for a #specName including different types of features"() {
        given:
        "The project build folder location is known"
        def buildDir = System.getProperty( 'project.buildDir', 'build' )

        when:
        "A Specification containing different types of features is run by Spock"
        use( reportCreator, HtmlReportCreatorSpec.PredictableTimeResponse ) {
            new Sputnik( specification ).run( new RunNotifier() )
        }

        then:
        "A nice template report to have been generated under the build directory"
        def reportFile = Paths.get( buildDir, 'spock-reports',
                specification.name + '.md' ).toFile()
        reportFile.parentFile.listFiles() && reportFile.exists()

        and:
        "The contents are functionally the same as expected"
        reportFile.text == expectedText( specName )

        where:
        specification | reportCreator
        FakeTest      | UseTemplateReportCreator
        VividFakeTest | UseTemplateShowCodeBlocksReportCreator
        specName = specification.getSimpleName()
    }

    String expectedText( String specName ) {
        this.class.getResource( "/${specName}.md" )
                .text
                .replace( '${projectUrl}', SpockReportExtension.PROJECT_URL )
                .replace( '${ds}', StringFormatHelper.ds as String )
                .replaceAll( "\r", '' )
    }

}

@Category( SpockReportExtension )
class UseTemplateReportCreator {
    static final templateReportCreator = new TemplateReportCreator(
            outputDir: System.getProperty( 'project.buildDir', 'build' ) + '/spock-reports',
            specTemplateFile: '/templateReportCreator/spec-template.md',
            reportFileExtension: 'md',
            summaryTemplateFile: '/templateReportCreator/summary-template.md',
            summaryFileName: 'summary.md'
    )

    SpecInfoListener createListener() {
        new SpecInfoListener( templateReportCreator )
    }

}

@Category( SpockReportExtension )
class UseTemplateShowCodeBlocksReportCreator {
    static final templateReportCreator = new TemplateReportCreator(
            outputDir: System.getProperty( 'project.buildDir', 'build' ) + '/spock-reports',
            specTemplateFile: '/templateReportCreator/spec-template.md',
            reportFileExtension: 'md',
            summaryTemplateFile: '/templateReportCreator/summary-template.md',
            summaryFileName: 'summary.md',
            showCodeBlocks: true )

    SpecInfoListener createListener() {
        new SpecInfoListener( templateReportCreator )
    }

}
