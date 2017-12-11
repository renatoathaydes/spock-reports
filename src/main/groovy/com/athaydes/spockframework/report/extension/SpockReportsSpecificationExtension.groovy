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
     * so it should only be called from a feature.
     * <p/>
     * This method may be called several times for each feature.
     *
     * @param self receiver
     * @param info to include in the feature report
     */
    static void reportInfo( Specification self, info ) {
        InfoContainer.add self.class.name, info
    }

}

@CompileStatic
class InfoContainer {

    private static final ITERATION_SEPARATOR = new Object()
    private static final Map<String, List> infoBySpecName = [ : ].asSynchronized()

    @PackageScope
    static void add( String name, item ) {
        infoBySpecName.get( name, [ ] ) << item
    }

    static void addSeparator( String name ) {
        add name, ITERATION_SEPARATOR
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

}
