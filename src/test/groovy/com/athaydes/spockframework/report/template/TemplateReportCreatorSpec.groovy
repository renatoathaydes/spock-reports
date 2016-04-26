package com.athaydes.spockframework.report.template

import com.athaydes.spockframework.report.FakeTest
import com.athaydes.spockframework.report.SpecInfoListener
import com.athaydes.spockframework.report.SpockReportExtension
import com.athaydes.spockframework.report.internal.HtmlReportCreatorSpec
import org.junit.runner.notification.RunNotifier
import org.spockframework.runtime.Sputnik
import spock.lang.Specification

import java.nio.file.Paths

class TemplateReportCreatorSpec extends Specification {

    def "A correct Template report is generated for a spec including different types of features"() {
        given:
        "The project build folder location is known"
        def buildDir = System.getProperty( 'project.buildDir', 'build' )

        when:
        "A Specification containing different types of features is run by Spock"
        use( UseTemplateReportCreator, HtmlReportCreatorSpec.PredictableTimeResponse ) {
            new Sputnik( FakeTest ).run( new RunNotifier() )
        }

        then:
        "A nice template report to have been generated under the build directory"
        def reportFile = Paths.get( buildDir, 'spock-reports',
                FakeTest.class.name + '.md' ).toFile()
        reportFile.exists()

        and:
        "The contents are functionally the same as expected"
        reportFile.text == expectedText()
    }

    String expectedText() {
        this.class.getResource( '/FakeTest.md' ).text.replace( '${projectUrl}', SpockReportExtension.PROJECT_URL ).replaceAll("\r",'')
    }

}

@Category( SpockReportExtension )
class UseTemplateReportCreator {
    static final templateReportCreator = new TemplateReportCreator()

    SpecInfoListener createListener() {
        configReportCreator templateReportCreator
        new SpecInfoListener( templateReportCreator )
    }

}
