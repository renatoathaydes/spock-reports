package com.athaydes.spockframework.report.internal

import com.athaydes.spockframework.report.util.Utils
import groovy.util.logging.Slf4j
import org.spockframework.util.Nullable

@Slf4j
class CssResource {

    final String css
    final boolean inlineCss
    final File cssFile

    CssResource( String css, boolean inlineCss, File cssFile ) {
        this.css = css
        this.inlineCss = inlineCss
        this.cssFile = cssFile
    }

    String getText() {
        def text = resource?.text
        if ( !text ) {
            log.warn( 'css resource seems to be empty: {}', resource )
            return ''
        }

        log.debug( 'Found css resource ({} characters long)', text.size() )

        if ( inlineCss ) {
            log.debug( "Inlining css in HTML report" )
            return text
        } else {
            log.debug( "Writing css file to {}", cssFile.absolutePath )
            try {
                cssFile.write text
            } catch ( e ) {
                log.warn( 'Unable to write CSS file to {} due to {}', text, e )
            }
            return cssFile.name
        }
    }

    @Nullable
    private URL getResource() {
        if ( Utils.isUrl( css ) ) urlResource
        else classPathResource
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
            log.info "The CSS classpath resource does not exist: {}", css
        null
    }

    @Nullable
    private URL getUrlResource() {
        log.debug 'Getting URL resource text: {}', css
        try {
            return new URL( css )
        } catch ( e ) {
            log.warn( "Failed to set CSS resource as the URL {} could not be read due to {}", css, e )
        }
        null
    }

}
