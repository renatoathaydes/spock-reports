package com.athaydes.spockframework.report.engine

import org.junit.platform.engine.DiscoverySelector
import org.junit.platform.testkit.engine.EngineExecutionResults
import org.junit.platform.testkit.engine.EngineTestKit
import org.junit.platform.testkit.engine.EventStatistics
import spock.lang.Specification

import java.util.function.Consumer

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass

trait CanRunSpockSpecs {

    void runSpec( Class<? extends Specification> specClass, Consumer<EventStatistics> assertions = null ) {
        if ( assertions ) execute( selectClass( specClass ), assertions )
        else execute( selectClass( specClass ) )
    }

    private static void execute( DiscoverySelector selector, Consumer<EventStatistics> statisticsConsumer ) {
        execute( selector )
                .testEvents()
                .debug()
                .assertStatistics( statisticsConsumer )
    }

    private static EngineExecutionResults execute( DiscoverySelector selector ) {
        return EngineTestKit
                .engine( "spock" )
                .selectors( selector )
                .execute()
    }

}
