package com.athaydes.spockframework.report

import com.athaydes.spockframework.report.internal.ConfigLoader
import com.athaydes.spockframework.report.internal.HtmlReportCreator
import com.athaydes.spockframework.report.internal.SpockReportsConfiguration
import com.athaydes.spockframework.report.template.TemplateReportCreator
import org.spockframework.runtime.model.SpecInfo
import spock.lang.Specification
import spock.lang.Unroll

/**
 *
 * User: Renato
 */
class SpockReportExtensionSpec extends Specification {

    def "The settings found in the config_properties file are read only once"() {
        given:
        "An instance of SpockReportExtension with a mocked out config loader"
        def mockReportCreator = Mock IReportCreator
        def extension = new SpockReportExtension() {
            @Override
            IReportCreator instantiateReportCreator( String reportCreatorClassName ) { mockReportCreator }
        }
        extension.configLoader = Mock ConfigLoader

        when:
        "Spock visits 10 spec"
        10.times {
            extension.start()
            extension.visitSpec( Mock( SpecInfo ) )
            extension.stop()
        }

        then:
        "The config is read once"
        1 * extension.configLoader.loadConfig( _ ) >> new Properties()
    }

    def "The settings found in the config_properties file are used to configure the report framework"() {
        given:
        "A set of valid and invalid properties emulating the properties file"
        def className = 'MockReportCreator'

        def props = new Properties()
        props.setProperty( IReportCreator.class.name, className )
        props.setProperty( className + '.customProp', 'customValue' )
        props.setProperty( "com.athaydes.spockframework.report.outputDir", "the-output-dir" )
        props.setProperty( "some.invalid.property", "invalid-value" )
        props.setProperty( IReportCreator.name, HtmlReportCreator.name )

        and:
        "A real ConfigLoader that uses the properties file"
        def configLoader = new ConfigLoader() {
            @Override
            Properties loadConfig( SpockReportsConfiguration config ) { props }
        }

        and:
        "A mock ReportCreator"
        def mockReportCreator = GroovyMock( IReportCreator )
        mockReportCreator.getClass() >> [ name: className ]
        mockReportCreator.hasProperty( _ as String ) >> { String name ->
            name in [ 'customProp', 'outputDir' ]
        }

        and:
        "A mock Spec"
        def mockSpecInfo = Mock( SpecInfo )

        and:
        "An instance of SpockReportExtension with mocked out config loader and reportCreator"
        def extension = new SpockReportExtension() {
            @Override
            IReportCreator instantiateReportCreator( String reportCreatorClassName ) { mockReportCreator }
        }
        extension.configLoader = configLoader

        when:
        "This extension framework is initiated by Spock visiting the mock spec"
        extension.start()
        extension.visitSpec( mockSpecInfo )
        extension.stop()

        then:
        "This extension added a SpecInfoListener to Spock's SpecInfo"
        1 * mockSpecInfo.addListener( _ as SpecInfoListener )

        and:
        "The ReportCreator was configured with the valid properties"
        1 * mockReportCreator.setOutputDir( "the-output-dir" )

        // this property does not exist in the report type, hence cannot be set
        0 * mockReportCreator.setCustomProp( "customValue" )

        and:
        "The ReportCreator is done"
        1 * mockReportCreator.done()
    }

    @Unroll( "More than one report creator can be specified in the properties when there are #description" )
    def "More than one report creator can be specified in the properties"() {
        given:
        "Properties specifying two report creators in a comma separated property value"
        def props = new Properties()
        props.setProperty( IReportCreator.name, iReportCreatorPropertyValue )

        and:
        "A real ConfigLoader that uses the properties file"
        def configLoader = new ConfigLoader() {
            @Override
            Properties loadConfig( SpockReportsConfiguration config ) { props }
        }

        and:
        "A couple of mock ReportCreators are created"
        def htmlReportCreator = Mock( HtmlReportCreator )
        def templateReportCreator = Mock( TemplateReportCreator )

        and:
        "A mock Spec"
        def mockSpecInfo = Mock( SpecInfo )

        and:
        "An instance of SpockReportExtension with mocked out config loader and reportCreators"
        def extension = new SpockReportExtension() {
            @Override
            IReportCreator instantiateReportCreator( String reportCreatorClassName ) {
                switch ( reportCreatorClassName ) {
                    case HtmlReportCreator.class.name: return htmlReportCreator
                    case TemplateReportCreator.class.name: return templateReportCreator
                    default: throw new IllegalArgumentException( "Unexpected creator requested" )
                }
            }
        }
        extension.configLoader = configLoader

        when:
        "This extension framework is initiated by Spock visiting the mock spec"
        extension.start()
        extension.visitSpec( mockSpecInfo )
        extension.stop()

        then:
        "This extension added a SpecInfoListener to Spock's SpecInfo"
        1 * mockSpecInfo.addListener( _ as SpecInfoListener )

        and:
        "The ReportCreators are both called"
        1 * htmlReportCreator.done()
        1 * templateReportCreator.done()

        where:
        iReportCreatorPropertyValue                                       | description
        "${HtmlReportCreator.name},${TemplateReportCreator.name}"         | "no spaces"
        " ${HtmlReportCreator.name},${TemplateReportCreator.name}"        | "a space at the start"
        "${HtmlReportCreator.name},${TemplateReportCreator.name} "        | "a space at the end"
        "${HtmlReportCreator.name} ,${TemplateReportCreator.name}"        | "a space in the middle"
        " ${HtmlReportCreator.name} ,${TemplateReportCreator.name} "      | "some spaces"
        "    ${HtmlReportCreator.name},${TemplateReportCreator.name}"     | "some tabs"
        " ${HtmlReportCreator.name} ,   ${TemplateReportCreator.name}   " | "lots of spaces and tabs"

    }

}
