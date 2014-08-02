package com.athaydes.spockframework.report

import com.athaydes.spockframework.report.internal.ConfigLoader
import com.athaydes.spockframework.report.internal.FeatureRun
import com.athaydes.spockframework.report.internal.SpecData
import com.athaydes.spockframework.report.internal.SpecProblem
import org.spockframework.runtime.IRunListener
import org.spockframework.runtime.extension.IGlobalExtension
import org.spockframework.runtime.model.ErrorInfo
import org.spockframework.runtime.model.FeatureInfo
import org.spockframework.runtime.model.IterationInfo
import org.spockframework.runtime.model.SpecInfo

/**
 *
 * User: Renato
 */
class SpockReportExtension implements IGlobalExtension {

	static final PROJECT_URL = 'https://github.com/renatoathaydes/spock-reports'
	static final String DEFAULT_OUTPUT_DIR = "build/outputDir"

	def configLoader = new ConfigLoader()
	String reportCreatorClassName
	final reportCreatorSettings = [ : ]
	String outputDir

	def firstVisit = true

	@Override
	void visitSpec( SpecInfo specInfo ) {
		if ( firstVisit ) {
			config()
			firstVisit = false
		}
		if ( reportCreatorClassName )
			try {
				def reportCreator = instantiateReportCreator()
				configReportCreator( reportCreator )
				specInfo.addListener new SpecInfoListener( reportCreator )

			} catch ( e ) {
				e.printStackTrace()
				println "Failed to create instance of $reportCreatorClassName: $e"
			}
	}

	void config() {
		println "Configuring ${this.class.name}"
		def config = configLoader.loadConfig()
		reportCreatorClassName = config.getProperty( IReportCreator.class.name )
		outputDir = config.getProperty( "com.athaydes.spockframework.report.outputDir", DEFAULT_OUTPUT_DIR )
		try {
			reportCreatorSettings << loadSettingsFor( reportCreatorClassName, config )
		} catch ( e ) {
			e.printStackTrace()
			println "Error configuring ${this.class.name}! $e"
		}
	}

	def instantiateReportCreator() {
		def reportCreatorClass = Class.forName( reportCreatorClassName )
		reportCreatorClass.asSubclass( IReportCreator ).newInstance()
	}

	def loadSettingsFor( String prefix, Properties config ) {
		Collections.list( config.propertyNames() ).grep { String key ->
			key.startsWith prefix + '.'
		}.collect { String key ->
			[ ( key - ( prefix + '.' ) ): config.getProperty( key ) ]
		}.collectEntries()
	}

	private void configReportCreator( IReportCreator reportCreator ) {
		reportCreator.outputDir = outputDir
		reportCreatorSettings.each { field, value ->
			reportCreator."$field" = value
		}
	}

}

class SpecInfoListener implements IRunListener {

	final IReportCreator reportCreator
	SpecData specData
	IterationInfo currentIteration
	long startT

	SpecInfoListener( IReportCreator reportCreator ) {
		this.reportCreator = reportCreator
	}

	@Override
	synchronized void beforeSpec( SpecInfo spec ) {
		//println "Before Spec ${spec.name}"
		specData = new SpecData( info: spec )
		startT = System.currentTimeMillis()
	}

	@Override
	void beforeFeature( FeatureInfo feature ) {
		//println "Feature: ${feature.name}"
		//println "Variables are: ${feature.parameterized ? feature.parameterNames : '[]' }"
		//println "Blocks text:"
		//feature.blocks.each { println "Block ${it.kind}: ${it.texts} : ${it.properties}" }
		specData.featureRuns << new FeatureRun( feature: feature )
	}

	@Override
	void beforeIteration( IterationInfo iteration ) {
		//println 'Iteration properties: ' + iteration.properties
		currentRun().failuresByIteration[ iteration ] = [ ]
		currentIteration = iteration
	}

	@Override
	void afterIteration( IterationInfo iteration ) {
		currentIteration = null
	}

	@Override
	void afterFeature( FeatureInfo feature ) {

	}

	@Override
	void afterSpec( SpecInfo spec ) {
		assert specData.info == spec
		specData.totalTime = System.currentTimeMillis() - startT
		reportCreator.createReportFor specData
		specData = null
	}

	@Override
	void error( ErrorInfo error ) {
		if ( !currentIteration ) throw new RuntimeException( 'No current iteration!' )
		currentRun().failuresByIteration[ currentIteration ] << new SpecProblem( error )
	}

	@Override
	void specSkipped( SpecInfo spec ) {
		// specInfo already knows if it's skipped
		println "Spec is skipped! ${spec.name}"
	}

	@Override
	void featureSkipped( FeatureInfo feature ) {
		println "Feature is skipped! ${feature.name}"
		// feature already knows if it's skipped
	}

	private FeatureRun currentRun() {
		specData.featureRuns.last()
	}

}
