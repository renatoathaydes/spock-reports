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
        examples | expected
        [ ] | true
        [ '' ] | true
        [ ' ' ] | true
        [ '', '' ] | true
        [ ' ', '  ' ] | true
        [ '', '', '     ' ] | true
        [ 'a' ] | false
        [ '', 'a' ] | false
        [ 'a', '' ] | false
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

}
