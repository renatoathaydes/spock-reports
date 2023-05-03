package com.athaydes.spockframework.report.internal

import groovy.transform.CompileStatic
import spock.lang.Narrative
import spock.lang.Title

@Title( "spock-reports TestHelper specification" )
@Narrative( "This specification ensures the helper works" )
final class TestHelper {

    static String minify( String xml ) {
        xml.replaceAll( /[\t\r\n]\s*/, '' ).replaceAll( />\s+</, '><' )
    }

    static void assertVerySimilar( String actualText, String expectedText ) {
        def actualLines = partition( actualText ).iterator()
        def expectedLines = partition( expectedText ).iterator()
        def currentLine = 1
        while ( actualLines.hasNext() ) {
            def actual = actualLines.next().trim()
            if ( expectedLines.hasNext() ) {
                def expected = expectedLines.next().trim()
                assert actual == expected: "At index ${actualLines.index}.\n" +
                        "  Expected: $expected\n" +
                        "  Actual  : $actual\n"
            } else {
                assert false: "At index ${actualLines.index}.\n" +
                        "  Expected: <end-of-file>\n" +
                        "  Actual  : $actual\n"
            }
            currentLine++
        }
        if ( expectedLines.hasNext() ) {
            def expected = expectedLines.next().trim()
            assert false: "At index ${expectedText.index}.\n" +
                    "  Expected: $expected\n" +
                    "  Actual  : <end-of-file>\n"
        }
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
        '\n\r \n\r \n\r '              | ''
    }

    private static Iterable<String> partition( String text ) {
        return new Iterable<String>() {
            static final int PARTITION_SIZE = 100

            @Override
            Iterator<String> iterator() {
                return new Iterator<String>() {
                    int index = 0

                    @CompileStatic
                    @Override
                    boolean hasNext() {
                        return index < text.length()
                    }

                    @CompileStatic
                    @Override
                    String next() {
                        def partition = text[ index..<Math.min( text.size(), index + PARTITION_SIZE ) ]
                        index += partition.size()
                        return partition
                    }
                }
            }
        }
    }

}
