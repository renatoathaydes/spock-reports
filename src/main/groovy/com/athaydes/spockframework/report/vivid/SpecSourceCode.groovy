package com.athaydes.spockframework.report.vivid

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.codehaus.groovy.ast.MethodNode
import org.spockframework.util.Nullable

@ToString
@Slf4j
@CompileStatic
class SpecSourceCode {

    private final Map<String, FeatureSourceCode> sourceCodeByFeatureName = [ : ]
    private final LinkedHashSet<SpecSourceCode> parents = [ ]

    void addParent( SpecSourceCode parent ) {
        parents << parent
    }

    void startBlock( MethodNode feature, String label, @Nullable String text ) {
        sourceCodeByFeatureName.get( feature.name, new FeatureSourceCode() ).startBlock( label, text )
    }

    void addStatement( MethodNode feature, String statement, int lineNumber ) {
        def currentFeature = sourceCodeByFeatureName[ feature.name ]
        if ( currentFeature ) {
            statement = removeIndent( statement )
            currentFeature.addStatement( statement, lineNumber )
        } else {
            log.debug( "Skipping statement on method {}, not a test method?", feature?.name )
        }
    }

    List<BlockCode> getBlocks( String featureName ) {
        List<BlockCode> result = sourceCodeByFeatureName[ featureName ]?.blocks
        if ( result == null ) {
            log.debug( 'Unable to find code for feature "{}", will try in parent Specs: {}', featureName, parents )

            Iterator<SpecSourceCode> parentIterator = parents.iterator()
            while ( result == null ) {
                if ( parentIterator.hasNext() ) {
                    def parent = parentIterator.next()
                    result = parent.getBlocks( featureName )
                } else {
                    break
                }
            }
        }
        return result ?: [ ]
    }

    static String removeIndent( String code ) {
        def lines = code.readLines()
        if ( lines.size() < 2 ) {
            return code
        }

        // do not use the first line because the first line never gets any indentation
        def firstTextIndexes = lines[ 1..-1 ].collect { String line -> line.findIndexOf { it != ' ' } }
        def minIndent = firstTextIndexes.min()

        if ( minIndent > 0 ) {
            def resultLines = [ lines[ 0 ] ] + lines[ 1..-1 ].collect { String line -> line.substring( minIndent ) }
            return resultLines.join( '\n' )
        } else {
            return code
        }
    }

}

@ToString
@CompileStatic
@PackageScope
class FeatureSourceCode {
    private final List<BlockCode> blocks = [ ]

    void startBlock( String label, @Nullable String text ) {
        blocks << new BlockCode( label, text, [ ], [ ] )
    }

    void addStatement( String statement, int lineNumber ) {
        def block = blocks.last()
        statement.split( '\n' ).eachWithIndex { String line, int index ->
            block.statements.add( line )
            block.lineNumbers.add( lineNumber + index )
        }
    }

    List<BlockCode> getBlocks() {
        return this.@blocks.asImmutable()
    }
}

@Canonical
class BlockCode {
    final String label
    @Nullable
    final String text
    final List<String> statements
    final List<Integer> lineNumbers
}
