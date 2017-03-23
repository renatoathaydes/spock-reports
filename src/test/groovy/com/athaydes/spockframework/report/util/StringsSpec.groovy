package com.athaydes.spockframework.report.util

import spock.lang.Specification

class StringsSpec extends Specification {

    def "The HtmlReportCreator knows when block texts have no content"() {
        when:
        "Block texts are checked for content"
        def result = Strings.isEmptyOrContainsOnlyEmptyStrings( examples )

        then:
        "Result is as expected"
        result == expected

        where:
        "The given examples are used"
        examples            | expected
        [ ]                 | true
        [ '' ]              | true
        [ ' ' ]             | true
        [ '', '' ]          | true
        [ ' ', '  ' ]       | true
        [ '', '', '     ' ] | true
        [ 'a' ]             | false
        [ '', 'a' ]         | false
        [ 'a', '' ]         | false
    }

    def "Recognizes valid URLs"() {
        when: 'We ask if a String is a URL'
        def result = Strings.isUrl( example )

        then: 'The expected answer is given'
        result == expected

        where:
        example                        | expected
        ''                             | false
        'h'                            | false
        'h:'                           | false
        '123'                          | false
        'not a URL'                    | false
        'some_text'                    | false
        'http://'                      | false
        'h://a'                        | true
        'h://&amp;/hi'                 | true
        'h://a.com'                    | true
        'http://hello.com'             | true
        'www.hi.com'                   | true
        'hi.com'                       | true
        'hi.com/p1/p2'                 | true
        'hi.com/p1/p2?v=true&y=a'      | true
        'hi.com/p1/p2#abc%20%x'        | true
        'http://hello.com/path1/path2' | true
        'www.hi.com/p1/p2/p3/p4/'      | true
        'file://hi/some_file.txt'      | true
        '192.168.16.1'                 | true
        '192.168.16.1:8080'            | true
        '192.168.16.1:8080/p1/p2'      | true
        '192.168.16.1:8080/p1/p2/'     | true
    }

}
