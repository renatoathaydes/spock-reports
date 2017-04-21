package com.athaydes.spockframework.report

import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class UnrolledSpec extends Specification {

    def "Not exampled-based feature"() {
        expect: '2 and 2 is 4'
        2 + 2 == 4
    }

    def "Example-based feature"() {
        expect: '#a + #b == #c'
        a + b == c

        where:
        a | b | c
        2 | 2 | 4
        1 | 4 | 5
    }

    def "Second Example-based feature"() {
        expect: '#a and #b is equal to #c'
        a + b == c

        where: 'a=#a, b=#b, c=#c'
        a | b | c
        1 | 2 | 3
        5 | 3 | 8
    }

}
