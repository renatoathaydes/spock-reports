package com.athaydes.spockframework.report

import org.spockframework.runtime.model.parallel.ExecutionMode
import spock.lang.Execution
import spock.lang.Rollup
import spock.lang.Specification
import spock.lang.Unroll

@Execution( ExecutionMode.CONCURRENT )
class SpecIncludingExtraInfo extends Specification {

    def setupSpec() {
        reportHeader '<div class="super-header">Report environment: currentOS</div>'
    }

    @Execution( ExecutionMode.CONCURRENT )
    def "Simple feature adding a list to the report"() {
        when: 'The report adds something to the report'
        sleep 5
        reportInfo( [ 1, 2, 3 ] )

        then: 'Show it in the report'
    }

    @Execution( ExecutionMode.CONCURRENT )
    def "Feature adding several items to the report"() {
        given: 'Some info is added to the report'
        reportInfo( 'Hello world' )

        when: 'More info is added'
        sleep 10
        reportInfo( 'More information here' )
        reportInfo( 'Even more now' )

        then: 'All of that info is shown'
    }

    @Execution( ExecutionMode.CONCURRENT )
    @Rollup
    def "Non-unrolled example-based feature adding info"() {
        when: 'Info is added on each iteration'
        sleep 5
        reportInfo( "The current iteration is $iteration (not-unrolled)" )

        then: 'It works'

        where:
        iteration << [ 0, 1, 2 ]
    }

    @Execution( ExecutionMode.CONCURRENT )
    @Unroll
    def "Unrolled example-based feature adding info"() {
        when: 'Info is added on each iteration'
        sleep 10
        reportInfo( "The current iteration is $iteration (unrolled)" )

        then: 'It works'

        where:
        iteration << [ 0, 1, 2 ]
    }

}
