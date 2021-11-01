package com.athaydes.spockframework.report

import com.athaydes.spockframework.report.extension.InfoContainer
import com.athaydes.spockframework.report.internal.ConfigLoader
import com.athaydes.spockframework.report.internal.FeatureRun
import com.athaydes.spockframework.report.internal.MultiReportCreator
import com.athaydes.spockframework.report.internal.SpecData
import com.athaydes.spockframework.report.internal.SpecProblem
import com.athaydes.spockframework.report.internal.SpockReportsConfiguration
import com.athaydes.spockframework.report.util.Utils
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.spockframework.runtime.IRunListener
import org.spockframework.runtime.extension.IGlobalExtension
import org.spockframework.runtime.model.ErrorInfo
import org.spockframework.runtime.model.FeatureInfo
import org.spockframework.runtime.model.IterationInfo
import org.spockframework.runtime.model.MethodKind
import org.spockframework.runtime.model.SpecInfo

import java.util.concurrent.atomic.AtomicBoolean

/**
 *
 * User: Renato
 */
@Slf4j
class SpockReportExtension implements IGlobalExtension {

    static final PROJECT_URL = 'https://github.com/renatoathaydes/spock-reports'

    private final AtomicBoolean initialized = new AtomicBoolean( false )

    //@Injected
    private SpockReportsConfiguration configuration

    protected ConfigLoader configLoader = new ConfigLoader()

    IReportCreator reportCreator

    @CompileStatic
    @Override
    void start() {
        if ( !initialized.getAndSet( true ) ) {
            log.info( "Got configuration from Spock: {}", configuration )
            log.debug "Configuring ${this.class.name}"
            def config = configLoader.loadConfig( configuration )

            // Read the class report property and exit if its not set
            String commaListOfReportClasses = config.remove( IReportCreator.name )
            if ( !commaListOfReportClasses ) {
                log.warn( "Missing property: ${IReportCreator.name} - no report classes defined" )
                return
            }

            // Create the IReportCreator instance(s) - skipping those that fail
            def reportCreators = commaListOfReportClasses.tokenize( ',' )
                    .collect { it.trim() }
                    .collect { instantiateReportCreatorAndApplyConfig( it, config ) }
                    .findAll { it != null }

            // If none were successfully created then exit
            if ( reportCreators.isEmpty() ) return

            // Assign the IReportCreator(s) - use the multi report creator only if necessary
            reportCreator = ( reportCreators.size() == 1 ) ? reportCreators[ 0 ] : new MultiReportCreator( reportCreators )
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
            log.warn "Not creating report for ${specInfo.name} as reportCreator is null"
        }
    }

    @CompileStatic
    IReportCreator instantiateReportCreator( String reportCreatorClassName ) {
        def reportCreatorClass = Class.forName( reportCreatorClassName )
        reportCreatorClass
                .asSubclass( IReportCreator )
                .getDeclaredConstructor()
                .newInstance()
    }

    @CompileStatic
    IReportCreator instantiateReportCreatorAndApplyConfig( String reportCreatorClassName, Properties config ) {
        // Given the IReportCreator class name then create it and apply config properties
        try {
            def reportCreator = instantiateReportCreator( reportCreatorClassName )
            configLoader.apply( reportCreator, config )
            return reportCreator
        } catch ( e ) {
            log.warn( "Failed to create instance of $reportCreatorClassName", e )
            return null
        }
    }

    // this method is patched by the UseTemplateReportCreator category and others
    SpecInfoListener createListener() {
        new SpecInfoListener( reportCreator )
    }

}

@Slf4j
@CompileStatic
class SpecInfoListener implements IRunListener {

    private final IReportCreator reportCreator
    private final Map<SpecInfo, SpecData> specs = [ : ].asSynchronized()
    long startT

    SpecInfoListener( IReportCreator reportCreator ) {
        this.reportCreator = reportCreator
    }

    @Override
    synchronized void beforeSpec( SpecInfo spec ) {
        SpecData specData = specs[ spec ]
        if ( specData != null ) {
            log.debug( 'Unexpected state: Current specData is {}, not done yet and already started with {}',
                    specData.info?.name, spec.name )
        }
        specData = new SpecData( spec )
        specs[ spec ] = specData
        log.debug( "Before spec: {}", Utils.getSpecClassName( specData ) )
        startT = System.currentTimeMillis()
    }

    @Override
    void beforeFeature( FeatureInfo feature ) {
        log.debug( "Before feature: {}", feature.name )
        SpecData specData
        synchronized ( specs ) {
            specData = specs.find { info, data -> feature.spec == info }?.value
        }
        if ( specData ) {
            specData.withFeatureRuns { it << new FeatureRun( feature ) }
        } else {
            log.warn( "Unable to find feature" )
        }
    }

    @Override
    void beforeIteration( IterationInfo iteration ) {
        log.debug( "Before iteration: {}", iteration.name )
        featureRunFor( iteration ).with {
            failuresByIteration[ iteration ] = [ ]
            timeByIteration[ iteration ] = System.nanoTime()
        }
    }

    @Override
    void afterIteration( IterationInfo iteration ) {
        log.debug( "After iteration: {}", iteration.name )
        featureRunFor( iteration ).with {
            Long startTime = timeByIteration[ iteration ]
            if ( !startTime ) {
                timeByIteration[ iteration ] = 0L
            } else {
                long totalTime = ( ( System.nanoTime() - startTime ) / 1_000_000L ).toLong()
                timeByIteration[ iteration ] = totalTime
            }
        }
        SpecData specData = specs[ iteration.feature.spec ]
        InfoContainer.addSeparator( Utils.getSpecClassName( specData ) )
    }

    @Override
    void afterFeature( FeatureInfo feature ) {
        log.debug( "After feature: {}", feature.name )
    }

    @Override
    void afterSpec( SpecInfo spec ) {
        // we don't need the spec anymore
        SpecData specData = specs.remove( spec )
        if ( specData == null ) {
            log.debug( 'Unexpected state: running afterSpec without having a specData for {}', spec.name )
            return
        }
        assert specData.info == spec
        log.debug( "After spec: {}", Utils.getSpecClassName( specData ) )
        specData.totalTime = System.currentTimeMillis() - startT
        reportCreator.createReportFor specData
    }

    @Override
    void error( ErrorInfo errorInfo ) {
        SpecData specData = specs[ errorInfo.method.feature.spec ]
        try {
            log.debug( "Error on spec {}, feature {}", specData == null
                    ? "<INITIALIZATION ERROR>"
                    : Utils.getSpecClassName( specData ),
                    errorInfo.method?.feature?.name ?: '<none>'
            )

            def noSpecData = specData == null
            def setupSpecError = !noSpecData && errorInfo.method?.kind == MethodKind.SETUP_SPEC

            if ( setupSpecError ) {
                log.debug( 'Error in setupSpec method' )
                specData.initializationError = errorInfo
            } else if ( noSpecData ) {
                log.debug( 'Error before Spec could be instantiated' )
                specData.initializationError = errorInfo
            } else {
                def iteration = errorInfo.method.iteration
                if ( iteration != null ) {
                    featureRunFor( iteration ).failuresByIteration[ iteration ] << new SpecProblem( errorInfo )
                } else {
                    log.debug( "Error in cleanupSpec method: {}", errorInfo.exception?.toString() )
                    specData.cleanupSpecError = errorInfo
                }
            }
        } catch ( Throwable e ) {
            // nothing we can do here
            e.printStackTrace()
        }
    }

    @Override
    void specSkipped( SpecInfo spec ) {
        beforeSpec( spec )
        log.debug( "Skipping specification {}", Utils.getSpecClassName( spec ) )

        def specFeatures = spec.features
        synchronized ( specFeatures ) {
            specFeatures.each { feature ->
                feature.skipped = true
                beforeFeature( feature )
                afterFeature( feature )
            }
        }

        afterSpec( spec )
    }

    @Override
    void featureSkipped( FeatureInfo feature ) {
        // feature already knows if it's skipped
    }

    private FeatureRun featureRunFor( IterationInfo iteration ) {
        def targetFeature = iteration.feature
        SpecData specData = specs[ targetFeature.spec ]
        def run = specData.withFeatureRuns { it.find { it.feature == targetFeature } }
        if ( run == null ) {
            return new FeatureRun( specData.info.features?.first() ?: dummyFeature() )
        }
        return run
    }

    private static FeatureInfo dummyFeature() {
        new FeatureInfo( name: '<No Feature initialized!>' )
    }

}
