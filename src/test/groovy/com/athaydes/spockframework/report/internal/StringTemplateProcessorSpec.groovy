package com.athaydes.spockframework.report.internal

import org.spockframework.runtime.model.FeatureInfo
import org.spockframework.runtime.model.IterationInfo
import spock.lang.Specification

class StringTemplateProcessorSpec extends Specification {

    def "Should replace hash-prefixed variables with the provided values"() {
        given:
        "A StringTemplateProcessor"
        def processor = new StringTemplateProcessor()

        when:
        "A String is processed"
        def iteration = new IterationInfo( new FeatureInfo( name: string ), index, variableValues as Object[], 1 )
        def result = processor.process( string, variableNames, iteration )

        then:
        "All hash-prefixed variables are replaced with the respective value"
        result == expectedString

        where:
        index | string                   | variableNames      | variableValues || expectedString
            0 | ''                       | [ ]                | [ ]            || ''
            1 | ''                       | [ 'a' ]            | [ 0 ]          || ''
            2 | 'spec 1'                 | [ ]                | [ ]            || 'spec 1'
            3 | 'spec a'                 | [ 'a' ]            | [ 1 ]          || 'spec a'
            4 | 'A nice Spec'            | [ 'nice', 'Spec' ] | [ 'x', 'y' ]   || 'A nice Spec'
            5 | 'Value #x == #y'         | [ 'x', 'y' ]       | [ 1, 2 ]       || 'Value 1 == 2'
            6 | 'Value #x == #y'         | [ 'x' ]            | [ 4 ]          || 'Value 4 == #Error:y'
            7 | 'Value #x == #y'         | [ 'z' ]            | [ 2 ]          || 'Value #Error:x == #Error:y'
            8 | 'Value #x1 == #y'        | [ 'x', 'y' ]       | [ 0, 1 ]       || 'Value #Error:x1 == 1'
            9 | 'Value #x==#y'           | [ 'x', 'y' ]       | [ 0, 1 ]       || 'Value 0==1'
           10 | 'Value #x,#y,#z x, y, z' | [ 'x', 'y', 'z' ]  | [ 0, 1, 2 ]    || 'Value 0,1,2 x, y, z'
    }

}
