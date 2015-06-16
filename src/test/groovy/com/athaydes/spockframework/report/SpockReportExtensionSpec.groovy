package com.athaydes.spockframework.report

import com.athaydes.spockframework.report.internal.ConfigLoader
import com.athaydes.spockframework.report.internal.HtmlReportCreator
import org.spockframework.runtime.model.SpecInfo
import spock.lang.Specification

/**
 *
 * User: Renato
 */
class SpockReportExtensionSpec extends Specification {

    def "The settings found in the config.properties file are read only once"() {
        given:
        "An instance of SpockReportExtension with a mocked out config loader"
        def extension = new SpockReportExtension()
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
        1 * extension.configLoader.loadConfig() >> new Properties()
    }

    def "The settings found in the config.properties file are used to configure the report framework"() {
        given:
        "A mock ReportCreator"
        def mockReportCreator = GroovyMock( IReportCreator )
        def className = 'MockReportCreator'
        mockReportCreator.getClass() >> [ name: className ]

        and:
        "A mock ConfigLoader"
        def mockConfigLoader = Mock( ConfigLoader )
        def props = new Properties()
        props.setProperty( IReportCreator.name, HtmlReportCreator.name )
        mockConfigLoader.loadConfig() >> props

        and:
        "A mock Spec"
        def mockSpecInfo = Mock( SpecInfo )

        and:
        "An instance of SpockReportExtension with mocked out config loader and reportCreator"
        def extension = new SpockReportExtension() {
            @Override
            def instantiateReportCreator() { mockReportCreator }
        }
        extension.configLoader = mockConfigLoader

        and:
        "A set of valid and invalid properties emulating the properties file"
        props.setProperty( IReportCreator.class.name, className )
        props.setProperty( className + '.customProp', 'customValue' )
        props.setProperty( "com.athaydes.spockframework.report.outputDir", "the-output-dir" )
        props.setProperty( "some.invalid.property", "invalid-value" )

        when:
        "This extension framework is initiated by Spock visiting the mock spec"
        extension.start()
        extension.visitSpec( mockSpecInfo )
        extension.stop()

        then:
        "The class of the implementation of ReportCreator was correctly set"
        extension.reportCreatorClassName == "MockReportCreator"

        and:
        "This extension added a SpecInfoListener to Spock's SpecInfo"
        1 * mockSpecInfo.addListener( _ )

        and:
        "The ReportCreator was configured with the valid properties"
        1 * mockReportCreator.setOutputDir( "the-output-dir" )
        1 * mockReportCreator.setCustomProp( "customValue" )
    }

}
