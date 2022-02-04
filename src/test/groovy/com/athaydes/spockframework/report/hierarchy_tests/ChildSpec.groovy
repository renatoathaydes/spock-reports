package com.athaydes.spockframework.report.hierarchy_tests

class ChildSpec extends ParentSpec {
    def 'child feature'() {
        expect:
        true
    }

    def 'override'() {
        expect:
        true
    }

}
