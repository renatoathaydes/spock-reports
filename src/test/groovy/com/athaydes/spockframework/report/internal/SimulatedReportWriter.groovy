package com.athaydes.spockframework.report.internal

import groovy.transform.CompileStatic

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@CompileStatic
class SimulatedReportWriter {

    static void main( String[] args ) {
        assert args.size() == 2, "Expected 2 args (name of file to write to, number of threads) but got $args"
        final file = new File( args.first() )
        final threadCount = args.last().toInteger()
        assert write( file, threadCount ).await( 10, TimeUnit.SECONDS ), "Did not finish writing within timeout"
    }

    static CountDownLatch write( File file, int threadCount ) {
        final counter = new CountDownLatch( threadCount )

        ( 1..threadCount ).each { n ->
            sleep 50 // pause quickly to make sure the separate JVMs Threads interleave
            Thread.start {
                final raf = new RandomAccessFile( file, 'rw' )
                ReportDataAggregator.withFileLock( raf ) {
                    def contents = ReportDataAggregator.readTextFrom( raf )
                    def numbers = contents.split( /\s/ )
                    def newNumber = numbers.last().toInteger() + 1
                    raf.write( " $newNumber".bytes )
                }
                counter.countDown()
            }
        }

        return counter
    }

}
