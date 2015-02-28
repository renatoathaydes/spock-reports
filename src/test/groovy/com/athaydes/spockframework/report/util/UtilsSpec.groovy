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

}
