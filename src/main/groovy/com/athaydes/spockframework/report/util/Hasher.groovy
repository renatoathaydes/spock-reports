package com.athaydes.spockframework.report.util

import groovy.transform.CompileStatic

@Singleton
@CompileStatic
class Hasher {

    String hash( String text ) {
        text.hashCode().toString()
    }

}
