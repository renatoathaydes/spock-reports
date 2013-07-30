package com.athaydes.spockframework.report

import com.athaydes.spockframework.report.internal.ConfigLoader
import com.athaydes.spockframework.report.internal.FeatureRun
import com.athaydes.spockframework.report.internal.SpecData
import org.junit.ComparisonFailure
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
class JUnitReportExtension implements IGlobalExtension {

	final configLoader = new ConfigLoader()
	Class reportCreatorClass
	final reportCreatorSettings = [ : ]

	static firstVisit = true

	@Override
	void visitSpec( SpecInfo specInfo ) {
		if ( firstVisit ) {
			config()
			firstVisit = false
		}
		if ( reportCreatorClass )
			try {
				specInfo.addListener new SpecInfoListener( instantiateReportCreator() )
			} catch ( e ) {
				println "Failed to create instance of $reportCreatorClass"
			}
	}

	void config( ) {
		println "Configuring ${this.class.name}"
		def config = configLoader.loadConfig()
		def reportCreatorClassName = config.getProperty( IReportCreator.class.name )
		try {
			def reportCreatorClass = Class.forName( reportCreatorClassName )
			if ( IReportCreator.isAssignableFrom( reportCreatorClass ) ) {
				this.reportCreatorClass = reportCreatorClass
				reportCreatorSettings << loadSettingsFor( reportCreatorClassName, config )
			} else {
				println "Class $reportCreatorClassName does not implement ${IReportCreator.class.name}"
			}
		} catch ( e ) {
			println "Error configuring ${this.class.name}! $e"
		}
	}

	def instantiateReportCreator( ) {
		def reportCreator = reportCreatorClass.newInstance()
		reportCreatorSettings.each { field, value ->
			reportCreator."$field" = value
		}
		reportCreator
	}

	def loadSettingsFor( String prefix, Properties config ) {
		Collections.list( config.propertyNames() ).grep { key ->
			key.startsWith prefix + '.'
		}.collect { key ->
			[ ( key - ( prefix + '.' ) ): config.getProperty( key ) ]
		}.collectEntries()
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
		//println "There is some error: ${error.exception}"
		//println "Error at: ${error.exception.stackTrace}"
		if ( !currentIteration ) throw new RuntimeException( 'No current iteration!' )
		switch ( classify( error ) ) {
			case 'FAILURE': currentRun().failuresByIteration[ currentIteration ] << error
				break
			case 'ERROR': currentRun().error = error.exception
		}
	}

	private classify( ErrorInfo error ) {
		( error.exception instanceof AssertionError ||
				error.exception instanceof ComparisonFailure ) ? 'FAILURE' : 'ERROR'
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

	private FeatureRun currentRun( ) {
		specData.featureRuns.last()
	}

}
