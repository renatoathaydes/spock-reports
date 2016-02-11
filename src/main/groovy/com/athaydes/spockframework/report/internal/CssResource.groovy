package com.athaydes.spockframework.report.internal

import groovy.util.logging.Slf4j
import org.spockframework.util.Nullable

@Slf4j
class CssResource {

    static final urlRegex = /[A-z]+:\/\/.+/

    final String css
    final boolean inlineCss
    final File cssFile

    CssResource( String css, boolean inlineCss, File cssFile ) {
        this.css = css
        this.inlineCss = inlineCss
        this.cssFile = cssFile
    }

    String getText() {
        def text = resource?.text ?: ''
        if ( inlineCss ) {
            return text
        } else {
            cssFile.write text
            return cssFile.name
        }
    }

    @Nullable
    private URL getResource() {
        switch ( css ) {
            case urlRegex: return urlResource
            default: return classPathResource
        }
    }

    @Nullable
    private URL getClassPathResource() {
        log.debug 'Getting classpath resource text: {}', css
        def cssResource = this.class.getResource( "/$css" )
        if ( cssResource )
            try {
                return cssResource
            } catch ( e ) {
                log.warn( "Failed to set CSS resource to {} due to {}", css, e )
            }
        else
            log.info "The CSS classpath resource does not exist: ${css}"
        null
    }

    @Nullable
    private URL getUrlResource() {
        log.debug 'Getting URL resource text: {}', css
        try {
            new URL( css )
        } catch ( e ) {
            log.warn( "Failed to set CSS resource as the URL {} could not be read due to {}", css, e )
        }
        null
    }

}
