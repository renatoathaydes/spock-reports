package com.athaydes.spockframework.report

import com.athaydes.spockframework.report.internal.ConfigLoader
import com.athaydes.spockframework.report.internal.FeatureRun
import com.athaydes.spockframework.report.internal.SpecData
import com.athaydes.spockframework.report.internal.SpecProblem
import groovy.util.logging.Log
import org.spockframework.runtime.IRunListener
import org.spockframework.runtime.extension.IGlobalExtension
import org.spockframework.runtime.model.ErrorInfo
import org.spockframework.runtime.model.FeatureInfo
import org.spockframework.runtime.model.IterationInfo
import org.spockframework.runtime.model.SpecInfo

import java.util.logging.Level

/**
 *
 * User: Renato
 */
@Log
class SpockReportExtension implements IGlobalExtension {

    static final PROJECT_URL = 'https://github.com/renatoathaydes/spock-reports'

    def configLoader = new ConfigLoader()
    Properties config
    String reportCreatorClassName
    String outputDir
    boolean hideEmptyBlocks = false

    IReportCreator reportCreator

    @Override
    void start() {
        if ( config == null ) {
            config()
        }
        if ( reportCreatorClassName && !reportCreator )
            try {
                reportCreator = instantiateReportCreator()
                configReportCreator( reportCreator )
            } catch ( e ) {
                log.log( Level.INFO, "Failed to create instance of $reportCreatorClassName", e )
            }
    }

    @Override
    void stop() {
        reportCreator?.done()
    }

    @Override
    void visitSpec( SpecInfo specInfo ) {
        if ( reportCreator != null ) {
            specInfo.addListener createListener()
        } else {
            log.info "Not creating report for ${specInfo.name} as reportCreator is null"
        }
    }

    SpecInfoListener createListener() {
        new SpecInfoListener( reportCreator )
    }

    void config() {
        log.info "Configuring ${this.class.name}"
        config = configLoader.loadConfig()
        reportCreatorClassName = config.getProperty( IReportCreator.name )
        outputDir = config.getProperty( ConfigLoader.PROP_OUTPUT_DIR )
        hideEmptyBlocks = Boolean.parseBoolean(
                config.getProperty( ConfigLoader.PROP_HIDE_EMPTY_BLOCKS )
        )
    }

    def instantiateReportCreator() {
        def reportCreatorClass = Class.forName( reportCreatorClassName )
        reportCreatorClass
                .asSubclass( IReportCreator )
                .newInstance()
    }

    private static loadSettingsFor( String prefix, Properties config ) {
        log.info "Loading settings for reportCreator of type $prefix"
        Collections.list( config.propertyNames() ).grep { String key ->
            key.startsWith prefix + '.'
        }.collect { String key ->
            [ ( key - ( prefix + '.' ) ): config.getProperty( key ) ]
        }.collectEntries()
    }

    private void configReportCreator( IReportCreator reportCreator ) {
        reportCreator.outputDir = outputDir
        reportCreator.hideEmptyBlocks = hideEmptyBlocks
        def reportCreatorSettings = [ : ]
        try {
            reportCreatorSettings << loadSettingsFor( reportCreator.class.name, config )
        } catch ( e ) {
            log.warning( "Error configuring ${reportCreator.class.name}! ${e}" )
        }

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
