package com.athaydes.spockframework.report.internal
/**
 *
 * User: Renato
 */
class KnowsWhenAndWhoRanTest {

    String whenAndWhoRanTest( StringFormatHelper stringFormatter ) {
        "Created on ${stringFormatter.toDateString( new Date() )}" +
                " by ${getUserName()}"
    }

    private String getUserName() {
        System.getProperty( 'user.name' )
    }
}
