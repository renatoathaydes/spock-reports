package com.athaydes.spockframework.report.internal

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import java.nio.charset.Charset
import java.util.regex.Pattern

/**
 * Static functions for aggregating report data even across different Java processes.
 *
 * This is necessary to support parallel builds.
 */
class ReportDataAggregator {

    static final AGGREGATED_DATA_FILE = 'aggregated_report.json'

    static final charset = Charset.forName( 'utf-8' )
    static final jsonParser = new JsonSlurper()

    static Map<String, Map> getAllAggregatedDataAndPersistLocalData( File dir, Map localData ) {
        final rawFile = new File( dir, AGGREGATED_DATA_FILE )
        rawFile.createNewFile() // ensure file exists before locking it

        final dataFile = new RandomAccessFile( rawFile, 'rw' )
        final fileLock = dataFile.channel.lock()

        try {
            def persistedData = readTextFrom( dataFile ) ?: '{}'
            def allData = jsonParser.parseText( persistedData ) + localData
            appendDataToFile( dataFile, localData )
            return allData.asImmutable()
        } finally {
            fileLock.channel().force( true ) // forces flushing before releasing the lock
            fileLock.release()
            dataFile.close()
        }
    }

    private static void appendDataToFile( RandomAccessFile file, Map localData ) {
        if ( localData.isEmpty() ) {
            return;
        }
        def toWrite = JsonOutput.toJson( localData )
        def pointer = file.filePointer
        if ( pointer > 1 ) {
            // move back by one byte so we can remove the last '}' character
            file.seek( pointer - 1 )
            // and replace the opening '{' of the appended json object with a ','
            toWrite = toWrite.replaceFirst( Pattern.quote( '{' ), ',' )
        }
        file.write( toWrite.getBytes( charset ) )
    }

    private static String readTextFrom( RandomAccessFile file ) {
        def buffer = new byte[8]
        def result = new StringBuilder( file.length() as int )

        int bytesRead
        while ( ( bytesRead = file.read( buffer ) ) > 0 ) {
            result.append( new String( buffer[ 0..( bytesRead - 1 ) ] as byte[], charset ) )
        }
        return result.toString()
    }

}
