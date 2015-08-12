package com.athaydes.spockframework.report.internal

import spock.lang.Specification

/**
 *
 * User: Renato
 */
class TestHelper extends Specification {

    static String minify( String xml ) {
        xml.replaceAll( /[\t\r\n]/, '' ).replaceAll( />\s+</, '><' )
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
        ' '                            | ' '
        '\t\t\t\t\t<hi></hi>\n\r<ho/>' | '<hi></hi><ho/>'
        '\n\r \n\r \n\r '              | '   '
    }

}
