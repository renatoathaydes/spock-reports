package com.athaydes.spockframework.report.hierarchy_tests

import spock.lang.Specification

class ParentSpec extends Specification {
    def "super feature"() {
        expect:
        true
    }

    def 'override'() {
        expect:
        true
    }
}
