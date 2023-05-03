package com.athaydes.spockframework.report

import spock.lang.Ignore
import spock.lang.Specification

@Ignore
class FullyIgnoredSpec extends Specification {
    def 'feature1'() {
        expect: true
    }

    def 'feature2'() {
        expect: true
    }
}
