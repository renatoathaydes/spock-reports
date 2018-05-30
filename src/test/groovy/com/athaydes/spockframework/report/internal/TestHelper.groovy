package com.athaydes.spockframework.report.internal

import junit.framework.ComparisonFailure
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title

/**
 *
 * User: Renato
 */
@Title( "spock-reports TestHelper specification" )
@Narrative( "This specification ensures the helper works" )
class TestHelper extends Specification {

    static String minify( String xml ) {
        xml.replaceAll( /[\t\r\n]/, '' ).replaceAll( />\s+</, '><' )
    }

    static void assertVerySimilar( String actual, String expected ) {

        int difference = -1

        def index = 0
        for ( item in [ expected.toCharArray(), actual.toCharArray() ].transpose() ) {
            def ( ca, cb ) = item
            if ( ca != cb ) {
                difference = index
                break
            }
            index++
        }

        if ( difference >= 0 ) {
            def snippetSize = 20
            index = Math.max( 0, difference - snippetSize )
            def aPart = expected[ index..<Math.min( difference + snippetSize, expected.size() ) ]
            def bPart = actual[ index..<Math.min( difference + snippetSize, actual.size() ) ]
            def errorIndex = Math.min( index, snippetSize )
            def error = "\n\"$aPart\" != \"$bPart\"\n" +
                    "${' ' * ( errorIndex + 1 )}^${' ' * ( aPart.size() + 5 )}^"

            throw new ComparisonFailure( error, expected, actual )
        }

        assert expected == actual
    }

    def "minimizeXml() Spec"() {
        expect:
        minify( normalXml ) == result

        where:
        normalXml                      | result
        ''                             | ''
        '\t'                           | ''
        '\r'                           | ''
        '\n'                           | ''
        '\t\r\n'                       | ''
        '\n\r\r\n\r\n\r\n'             | ''
        'Hi'                           | 'Hi'
        '<a>  </a> <b> </b>'           | '<a></a><b></b>'
        ' '                            | ' '
        '\t\t\t\t\t<hi></hi>\n\r<ho/>' | '<hi></hi><ho/>'
        '\n\r \n\r \n\r '              | '   '
    }

}
