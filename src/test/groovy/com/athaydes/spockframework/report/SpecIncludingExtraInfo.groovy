package com.athaydes.spockframework.report

import spock.lang.Specification
import spock.lang.Unroll

class SpecIncludingExtraInfo extends Specification {

    def setupSpec() {
        reportHeader '<div class="super-header">Report environment: currentOS</div>'
    }

    def "Simple feature adding a list to the report"() {
        when: 'The report adds something to the report'
        reportInfo( [ 1, 2, 3 ] )

        then: 'Show it in the report'
    }

    def "Feature adding several items to the report"() {
        given: 'Some info is added to the report'
        reportInfo( 'Hello world' )

        when: 'More info is added'
        reportInfo( 'More information here' )
        reportInfo( 'Even more now' )

        then: 'All of that info is shown'
    }

    def "Non-unrolled example-based feature adding info"() {
        when: 'Info is added on each iteration'
        reportInfo( "The current iteration is $iteration" )

        then: 'It works'

        where:
        iteration << [ 0, 1, 2 ]
    }

    @Unroll
    def "Unrolled example-based feature adding info"() {
        when: 'Info is added on each iteration'
        reportInfo( "The current iteration is $iteration" )

        then: 'It works'

        where:
        iteration << [ 0, 1, 2 ]
    }

}
