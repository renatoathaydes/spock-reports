package com.athaydes.spockframework.report

import com.athaydes.spockframework.report.internal.FeatureRun
import com.athaydes.spockframework.report.internal.HtmlReportCreator
import com.athaydes.spockframework.report.internal.SpecData
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

	IReportCreator reportCreator = new HtmlReportCreator()

	@Override
	void visitSpec( SpecInfo specInfo ) {
		specInfo.addListener new SpecInfoListener( reportCreator )
	}

}

class SpecInfoListener implements IRunListener {

	final IReportCreator reportCreator
	SpecData specData
	IterationInfo currentIteration

	SpecInfoListener( IReportCreator reportCreator ) {
		this.reportCreator = reportCreator
	}

	@Override
	synchronized void beforeSpec( SpecInfo spec ) {
		println "Before Spec ${spec.name}"
		specData = new SpecData( info: spec )
	}

	@Override
	void beforeFeature( FeatureInfo feature ) {
		println "Feature: ${feature.name}"
		println "Variables are: ${feature.parameterized ? feature.parameterNames : '[]' }"
		println "Blocks text:"
		feature.blocks.each { println "Block ${it.kind}: ${it.texts} : ${it.properties}" }
		specData.featureRuns << new FeatureRun( feature: feature )
	}

	@Override
	void beforeIteration( IterationInfo iteration ) {
		println "Before Iteration : ${iteration.dataValues}"
		println 'Iteration properties: ' + iteration.properties
		currentRun().errorsByIteration[ iteration ] = [ ]
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
		reportCreator.createReportFor specData
		specData = null
	}

	@Override
	void error( ErrorInfo error ) {
		println "There is some error: ${error.exception}"
		if ( !currentIteration ) throw new RuntimeException( 'No current iteration!' )
		currentRun().errorsByIteration[ currentIteration ] << error
	}

	@Override
	void specSkipped( SpecInfo spec ) {
		// specInfo already knows if it's skipped
	}

	@Override
	void featureSkipped( FeatureInfo feature ) {
		// feature already knows if it's skipped
	}

	private FeatureRun currentRun( ) {
		specData.featureRuns.last()
	}

}
