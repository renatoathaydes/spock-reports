package com.athaydes.spockframework.report.util

import spock.lang.Specification

class UtilsSpec extends Specification {

    def "The success rate for all specs should be correctly calculated given all results"() {
        when:
        "The successRate is calculated"
        def result = Utils.successRate( total, reproved )

        then:
        "The expected result is obtained"
        GroovyTestCase.assertEquals result, expectedSuccessRate, 1E-9

        where:
        total | reproved | expectedSuccessRate
        0     | 0        | 1.0
        1     | 0        | 1.0
        1     | 1        | 0.0
        2     | 1        | 0.5
        100   | 30       | 0.7
        30    | 20       | ( 1 / 3 ).doubleValue()
        2     | 3        | 0.0
        10    | 10034    | 0.0
    }

    def "The HtmlReportCreator knows when block texts have no content"() {
        when:
        "Block texts are checked for content"
        def result = Utils.isEmptyOrContainsOnlyEmptyStrings( examples )

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


    def "The aggregate data for a sequence of test results should be computed correctly"() {
        when:
        "I request the aggregate data to be recomputed"
        def result = Utils.aggregateStats( aggregatedData )

        then:
        "The expected result is obtained"
        result == expected

        where:
        aggregatedData | expected
        [
                'A': [ failures   : 1, errors: 2,
                       skipped    : 0, totalRuns: 10,
                       successRate: 0.7, time: 1.25 ],
                'B': [ failures   : 6, errors: 4,
                       skipped    : 0, totalRuns: 20,
                       successRate: 0.5, time: 2.0 ],
                'C': [ failures   : 0, errors: 0,
                       skipped    : 0, totalRuns: 1,
                       successRate: 1.0, time: 3.0 ],
                'D': [ failures   : 0, errors: 0,
                       skipped    : 2, totalRuns: 3,
                       successRate: 1.0, time: 2.0 ]
        ]              | [ total: 4, passed: 2, failed: 2, fFails: 7, fErrors: 6, time: 8.25, successRate: 0.5 ]
    }

    def "Recognizes valid URLs"() {
        when: 'We ask if a String is a URL'
        def result = Utils.isUrl( example )

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
