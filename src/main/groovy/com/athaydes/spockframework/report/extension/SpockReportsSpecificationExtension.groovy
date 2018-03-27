package com.athaydes.spockframework.report.extension

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
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
        InfoContainer.add self.class.name, info
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
        InfoContainer.addHeader self.class.name, header
    }

}

@CompileStatic
class InfoContainer {

    private static final ITERATION_SEPARATOR = new Object()
    private static final Map<String, List> headerBySpecName = [ : ].asSynchronized()
    private static final Map<String, List> infoBySpecName = [ : ].asSynchronized()

    @PackageScope
    static void addHeader( String name, item ) {
        headerBySpecName.get( name, [ ] ) << item
    }

    @PackageScope
    static void add( String name, item ) {
        infoBySpecName.get( name, [ ] ) << item
    }

    static void addSeparator( String name ) {
        add name, ITERATION_SEPARATOR
    }

    static List getHeadersFor( String specName ) {
        headerBySpecName.remove( specName ) ?: [ ]
    }

    static List getNextInfoFor( String specName ) {
        def info = infoBySpecName[ specName ]
        if ( info == null ) {
            return [ ]
        }
        def result = [ ]
        def iterator = info.iterator()

        while ( iterator.hasNext() ) {
            def item = iterator.next()
            iterator.remove()
            if ( item == ITERATION_SEPARATOR ) {
                break
            } else {
                result << item
            }
        }
        if ( info.isEmpty() ) {
            infoBySpecName.remove( specName )
        }
        result
    }

    static List getAllExtraInfoFor( String specName ) {
        infoBySpecName.remove( specName ) ?: [ ]
    }

    static void resetSpecData( String specName, List headers, List extraInfo ) {
        headerBySpecName[ specName ] = headers
        infoBySpecName[ specName ] = extraInfo
    }

}
