package com.athaydes.spockframework.report.extension

import com.athaydes.spockframework.report.util.Utils
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.util.logging.Slf4j
import org.spockframework.runtime.model.FeatureInfo
import org.spockframework.runtime.model.IterationInfo
import spock.lang.Specification

@CompileStatic
class SpockReportsSpecificationExtension {

    /**
     * Add information to the Spock report.
     * <p/>
     * The provided object's String representation will be included in the feature report,
     * so this method should only be called from a feature.
     * <p/>
     * This method may be called several times for each feature.
     *
     * @param self receiver
     * @param info to include in the feature report
     */
    static void reportInfo( Specification self, info ) {
        InfoContainer.add self, info
    }

    /**
     * Add header to the Spock report.
     * <p/>
     * The provided object's String representation will be included in the Specification's report header,
     * so this method should normally be called from the {@code setupSpec} method.
     *
     * @param self
     * @param header to include in the Specification report
     */
    static void reportHeader( Specification self, header ) {
        InfoContainer.addHeader self, header
    }

}

@Slf4j
@CompileStatic
class InfoContainer {

    private static final Map<String, List> headerBySpecName = [ : ].asSynchronized()
    private static final Map<String, List> infoBySpecName = [ : ].asSynchronized()

    private static String keyFor( String specName,
                                  FeatureInfo feature,
                                  IterationInfo iteration ) {
        def index = iteration && Utils.isUnrolled( feature ) ? iteration.iterationIndex : -1
        "$specName${feature?.name}$index"
    }

    @PackageScope
    static void addHeader( Specification spec, item ) {
        headerBySpecName.get( spec.class.name, [ ] ) << item
    }

    @PackageScope
    static void add( Specification spec, item ) {
        try {
            def key = keyFor( spec.class.name,
                    spec.specificationContext.currentFeature,
                    spec.specificationContext.currentIteration )
            infoBySpecName.get( key, [ ] ) << item
        } catch ( e ) {
            log.debug( "Unable to add info to report, will add it as header instead: {}. " +
                    "Problem: {}", item, e )
            addHeader( spec, item )
        }
    }

    static List getHeadersFor( String specName ) {
        headerBySpecName.remove( specName ) ?: [ ]
    }

    static List getNextInfoFor( String specName,
                                FeatureInfo feature,
                                IterationInfo iteration ) {
        def key = keyFor( specName, feature, iteration )
        infoBySpecName.remove( key ) ?: [ ]
    }

    static void resetSpecData( String specName, List headers ) {
        headerBySpecName[ specName ] = headers
    }

}
