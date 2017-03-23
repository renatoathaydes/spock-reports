package com.athaydes.spockframework.report.util

import com.athaydes.spockframework.report.internal.SpecData
import org.spockframework.runtime.model.SpecInfo
import org.spockframework.util.Nullable

import java.nio.file.Paths

final class Files {

    static File createDir( String name ) {
        def dir = new File( name )
        dir.mkdirs()
        return dir
    }

    static boolean existsOrCanCreate( File file ) {
        file?.exists() || file?.mkdirs()
    }

    static String getSpecClassName(SpecData data ) {
        data.info.description?.className ?: specNameFromFileName( data.info )
    }

    static String specNameFromFileName(SpecInfo specInfo ) {
        def fileName = specInfo.filename
        def lastDotInFileName = fileName.lastIndexOf( '.' )
        def name = lastDotInFileName > 0 ? fileName.substring( 0, lastDotInFileName ) : fileName
        return specInfo.package + name
    }

    @Nullable
    static File getSpecFile(String testSourceRoots, SpecData data ) {
        def existingRoots = findRoots(testSourceRoots)
        for ( File root in existingRoots ) {
            List<String> pathParts = data.info.package.split( /\./ ).toList() + [ data.info.filename ]
            def specFile = Paths.get( root.absolutePath, *pathParts ).toFile()
            if ( specFile.isFile() ) {
                return specFile
            }
        }
        return null
    }

    private static List<File> findRoots(String sourceRoots) {
        return sourceRoots.split(File.pathSeparator).collect { root ->
            if (root) {
                def dir = new File(root)
                if (dir.isDirectory()) {
                    return dir
                }
            }
            null
        }.findAll { it != null }
    }

}
