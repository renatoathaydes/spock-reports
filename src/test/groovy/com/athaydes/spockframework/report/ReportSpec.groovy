package com.athaydes.spockframework.report

import com.athaydes.spockframework.report.internal.KnowsWhenAndWhoRanTest
import groovy.text.SimpleTemplateEngine
import spock.lang.Specification

/**
 *
 * User: Renato
 */
abstract class ReportSpec extends Specification {
    static final String TEST_USER_NAME = 'me'
    static final String DATE_TEST_RAN = 'today'

    KnowsWhenAndWhoRanTest mockKnowsWhenAndWhoRanTest() {
        def mockWhenAndWho = Mock( KnowsWhenAndWhoRanTest )
        mockWhenAndWho.whenAndWhoRanTest( _ ) >> "Created on $DATE_TEST_RAN by $TEST_USER_NAME"
        return mockWhenAndWho
    }

    String replacePlaceholdersInRawHtml( String rawHtml, Map binding ) {
        def templateEngine = new SimpleTemplateEngine()
        templateEngine.createTemplate( rawHtml ).make( binding ).toString()
    }
}
