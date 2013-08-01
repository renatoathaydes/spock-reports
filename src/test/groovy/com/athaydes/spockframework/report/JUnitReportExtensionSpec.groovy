package com.athaydes.spockframework.report

import com.athaydes.spockframework.report.internal.ConfigLoader
import org.spockframework.runtime.model.SpecInfo
import spock.lang.Specification

/**
 *
 * User: Renato
 */
class JUnitReportExtensionSpec extends Specification {

	def "The settings found in the config.properties file are read only once"( ) {
		given:
		"An instance of JUnitReportExtension with a mocked out config loader"
		def extension = new JUnitReportExtension()
		def callsCount = 0
		extension.configLoader = [ loadConfig: { callsCount++; new Properties() } ] as ConfigLoader

		when:
		"Spock visits one spec"
		extension.visitSpec( Mock( SpecInfo ) )

		then:
		"The config is read once"
		callsCount == 1

		when:
		"Spock visits another 10 specs"
		10.times { extension.visitSpec( Mock( SpecInfo ) ) }

		then:
		"The config has still been read only once"
		callsCount == 1
	}

	def "The settings found in the config.properties file are used to configure the report framework"( ) {
		given:
		"A mock ReportCreator"
		def mockReportCreator = GroovyMock( IReportCreator )

		and:
		"A mock ConfigLoader"
		def mockConfigLoader = Mock( ConfigLoader )
		def props = new Properties()
		mockConfigLoader.loadConfig() >> props

		and:
		"A mock Spec"
		def mockSpecInfo = Mock( SpecInfo )

		and:
		"An instance of JUnitReportExtension with mocked out config loader and reportCreator"
		def extension = new JUnitReportExtension() {
			@Override
			def instantiateReportCreator( ) { mockReportCreator }
		}
		extension.configLoader = mockConfigLoader

		and:
		"A set of valid and invalid properties emulating the properties file"
		props.setProperty( IReportCreator.class.name, "MockReportCreator" )
		props.setProperty( "MockReportCreator" + '.customProp', 'customValue' )
		props.setProperty( "com.athaydes.spockframework.report.outputDir", "the-output-dir" )
		props.setProperty( "some.invalid.property", "invalid-value" )

		when:
		"This extension framework is initiated by Spock visiting the mock spec"
		extension.visitSpec( mockSpecInfo )

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
