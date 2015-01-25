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

	def configLoader = new ConfigLoader()
	String reportCreatorClassName
	final reportCreatorSettings = [ : ]
	String outputDir
	boolean hideEmptyBlocks = false
	boolean silenceOutput = true

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
				if(!silenceOutput) {
					e.printStackTrace()
					println "Failed to create instance of $reportCreatorClassName: $e"
				}
			}
	}

	void config() {
		def config = configLoader.loadConfig()
		reportCreatorClassName = config.getProperty( IReportCreator.class.name )
		outputDir = config.getProperty( "com.athaydes.spockframework.report.outputDir" )
		hideEmptyBlocks = Boolean.parseBoolean(
				config.getProperty( "com.athaydes.spockframework.report.hideEmptyBlocks" )
		)
		silenceOutput = Boolean.parseBoolean(
				config.getProperty( 'com.athaydes.spockframework.silenceOutput' ) ?: 'true'
		)
		if(!silenceOutput)
			println "Configuring ${this.class.name}"		

		try {
			reportCreatorSettings << loadSettingsFor( reportCreatorClassName, config )
		} catch ( e ) {
			if(!silenceOutput) {
				e.printStackTrace()
				println "Error configuring ${this.class.name}! $e"
			}
		}
	}

	def instantiateReportCreator() {
		def reportCreatorClass = Class.forName( reportCreatorClassName )
		def obj = reportCreatorClass.asSubclass( IReportCreator ).newInstance()
		obj.silenceOutput = silenceOutput
		obj
	}

	private static loadSettingsFor( String prefix, Properties config ) {
		Collections.list( config.propertyNames() ).grep { String key ->
			key.startsWith prefix + '.'
		}.collect { String key ->
			[ ( key - ( prefix + '.' ) ): config.getProperty( key ) ]
		}.collectEntries()
	}

	private void configReportCreator( IReportCreator reportCreator ) {
		reportCreator.outputDir = outputDir
		reportCreator.hideEmptyBlocks = hideEmptyBlocks
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
		specData = new SpecData( info: spec )
		startT = System.currentTimeMillis()
	}

	@Override
	void beforeFeature( FeatureInfo feature ) {
		specData.featureRuns << new FeatureRun( feature: feature )
	}

	@Override
	void beforeIteration( IterationInfo iteration ) {
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
        def iteration = currentIteration ?: dummySpecIteration()
        currentRun().failuresByIteration[ iteration ] << new SpecProblem( error )
	}

	@Override
	void specSkipped( SpecInfo spec ) {
		// specInfo already knows if it's skipped
	}

	@Override
	void featureSkipped( FeatureInfo feature ) {
		// feature already knows if it's skipped
	}

    private FeatureRun currentRun() {
        if ( specData.featureRuns.empty ) {
            specData.featureRuns.add new FeatureRun( feature: specData.info.features?.first() ?: dummyFeature() )
        }
        specData.featureRuns.last()
    }

    private IterationInfo dummySpecIteration() {
        def currentRun = currentRun()
        def iteration = new IterationInfo( currentRun.feature, [ ] as Object[], 1 )
        iteration.name = '<No Iteration!>'
        currentRun.failuresByIteration.put( iteration, [ ] )
        iteration
    }

    private static FeatureInfo dummyFeature() {
        new FeatureInfo( name: '<No Feature initialized!>' )
    }

}
