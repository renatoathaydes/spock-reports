package com.athaydes.spockframework.report.util

import com.athaydes.spockframework.report.extension.InfoContainer
import com.athaydes.spockframework.report.internal.FailureKind
import com.athaydes.spockframework.report.internal.FeatureRun
import com.athaydes.spockframework.report.internal.SpecData
import com.athaydes.spockframework.report.internal.SpecProblem
import org.spockframework.runtime.model.BlockKind
import org.spockframework.runtime.model.FeatureInfo
import org.spockframework.runtime.model.IterationInfo
import org.spockframework.runtime.model.SpecInfo
import org.spockframework.util.Nullable
import spock.lang.PendingFeature
import spock.lang.Specification
import spock.lang.Unroll

import java.lang.annotation.Annotation
import java.nio.file.Paths
import java.util.regex.Pattern

class Utils {

    public static final Map block2String = [
            ( BlockKind.SETUP )  : 'Given:',
            'given'              : 'Given:',
            'setup'              : 'Given:',
            ( BlockKind.CLEANUP ): 'Cleanup:',
            'cleanup'            : 'Cleanup:',
            ( BlockKind.THEN )   : 'Then:',
            'then'               : 'Then:',
            ( BlockKind.EXPECT ) : 'Expect:',
            'expect'             : 'Expect:',
            ( BlockKind.WHEN )   : 'When:',
            'when'               : 'When:',
            ( BlockKind.WHERE )  : 'Where:',
            'where'              : 'Where:',
            'and'                : 'And:',
            'examples'           : 'Examples:'
    ]

    private static final Pattern urlPattern

    static {
        def urlRegex = "\\(?\\b([A-z]+://|[A-z0-9]+[.])[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_()|]"
        urlPattern = Pattern.compile( urlRegex )
    }

    static File createDir( String outputDir ) {
        def reportsDir = new File( outputDir )
        reportsDir.mkdirs()
        reportsDir
    }

    static double successRate( int total, int reproved ) {
        double dTotal = total
        double dReproved = reproved
        Math.min( 1.0D, Math.max( 0.0D, ( dTotal > 0D ? ( dTotal - dReproved ) / total : 1.0D ) ) )
    }

    static Map stats( SpecData data ) {
        def failures = countProblems( data.featureRuns, this.&isFailure )
        def errors = countProblems( data.featureRuns, this.&isError )
        def skipped = data.info.allFeaturesInExecutionOrder.count { FeatureInfo f -> isSkipped( f ) }
        def total = countFeatures( data.featureRuns )
        def successRate = successRate( total, ( errors + failures ).toInteger() )
        [ failures   : failures, errors: errors, skipped: skipped, totalRuns: total,
          successRate: successRate, time: data.totalTime ]
    }

    static Map aggregateStats( Map<String, Map> aggregatedData ) {
        def result = [ total: 0, passed: 0, failed: 0, fFails: 0, fErrors: 0, time: 0.0 ]
        aggregatedData.values().each { Map json ->
            def stats = json.stats
            def isFailure = stats.failures + stats.errors > 0
            result.total += 1
            result.passed += ( isFailure ? 0 : 1 )
            result.failed += ( isFailure ? 1 : 0 )
            result.fFails += stats.failures
            result.fErrors += stats.errors
            result.time += stats.time
        }
        result.successRate = successRate( result.total, result.failed )
        result
    }

    static boolean isEmptyOrContainsOnlyEmptyStrings( List<String> strings ) {
        !strings || strings.every { String it -> it.trim() == '' }
    }

    static boolean isUnrolled( FeatureInfo feature ) {
        feature.spec?.isAnnotationPresent( Unroll ) ||
                feature.description?.annotations?.any { Annotation a -> a.annotationType() == Unroll } ?: false
    }

    static boolean isFailure( SpecProblem problem ) {
        problem.kind == FailureKind.FAILURE
    }

    static boolean isError( SpecProblem problem ) {
        problem.kind == FailureKind.ERROR
    }

    static boolean isSkipped( FeatureInfo featureInfo ) {
        // pending features are not marked as skipped but they are always skipped or fail
        featureInfo.skipped || featureInfo.description.getAnnotation( PendingFeature )
    }

    static int countFeatures( List<FeatureRun> runs, Closure featureFilter = { true } ) {
        runs.findAll( featureFilter ).inject( 0 ) { int count, FeatureRun fr ->
            if ( isSkipped( fr.feature ) ) count
            else count + ( isUnrolled( fr.feature ) ? fr.iterationCount() : 1 )
        } as int
    }

    static int countProblems( List<FeatureRun> runs, Closure problemFilter ) {
        runs.inject( 0 ) { int count, FeatureRun fr ->
            def allProblems = fr.failuresByIteration.values().flatten()
            count + ( isUnrolled( fr.feature ) ?
                    allProblems.count( problemFilter ) :
                    allProblems.any( problemFilter ) ? 1 : 0 )
        } as int
    }

    static String iterationsResult( FeatureRun run ) {
        def totalErrors = run.failuresByIteration.values().count { List it -> !it.empty }
        "${run.iterationCount() - totalErrors}/${run.iterationCount()} passed"
    }

    static List<Map> problemsByIteration( Map<IterationInfo, List<SpecProblem>> failures ) {
        failures.inject( [ ] ) { List<Map> acc, iteration, List<SpecProblem> failureList ->
            def allErrors = failureList.collect { SpecProblem it -> it.failure.exception }
            acc << [ dataValues: iteration.dataValues, errors: allErrors ]
        }
    }

    static <A extends Annotation> A specAnnotation( SpecData data, Class<A> annotation ) {
        data.info.description?.testClass?.getAnnotation( annotation )
    }

    static List nextSpecExtraInfo( SpecData data ) {
        InfoContainer.getNextInfoFor( getSpecClassName( data ) )
    }

    static List specHeaders( SpecData data ) {
        InfoContainer.getHeadersFor( getSpecClassName( data ) )
    }

    static boolean isUrl( String text ) {
        text ==~ urlPattern
    }

    static Map createAggregatedData( List<FeatureInfo> executedFeatures,
                                     List<FeatureInfo> ignoredFeatures,
                                     Map stats,
                                     String specTitle = '',
                                     String narrative = '' ) {
        [
                executedFeatures: executedFeatures?.name?.sort() ?: [ ],
                ignoredFeatures : ignoredFeatures?.name?.sort() ?: [ ],
                stats           : stats,
                title           : specTitle,
                narrative       : narrative,
        ]
    }

    static String featureNameFrom( FeatureInfo feature, IterationInfo iteration, int index ) {
        if ( feature.iterationNameProvider && iteration.dataValues?.length > 0 ) {
            def name = feature.iterationNameProvider.getName( iteration )

            // reset the index instance to fix #70
            def nameMatcher = name =~ /(.*)\[\d+\]$/
            if ( nameMatcher.matches() ) {
                def rawName = nameMatcher.group( 1 )
                return "$rawName [$index]"
            } else {
                return name
            }
        } else {
            return feature.name
        }
    }

    static String getSpecClassName( SpecData data ) {
        data.info.description?.className ?: specNameFromFileName( data.info )
    }

    static List<String> getParentSpecNames( String className ) {
        def result = [ ]
        Class<?> type
        try {
            type = Class.forName( className )
        } catch ( ignore ) {
            return result
        }

        if ( !Specification.isAssignableFrom( type ) ) {
            return result
        }

        type = type.superclass
        while ( type && type != Specification && Specification.isAssignableFrom( type ) ) {
            result << type.name
            type = type.superclass
        }

        return result
    }

    static String specNameFromFileName( SpecInfo specInfo ) {
        def fileName = specInfo.filename

        def lastDotInFileName = fileName.lastIndexOf( '.' )
        def name = lastDotInFileName > 0 ? fileName.substring( 0, lastDotInFileName ) : fileName

        return specInfo.package + '.' + name
    }

    @Nullable
    static File getSpecFile( String testSourceRoots, SpecData data ) {
        def existingRoots = testSourceRoots.split( File.pathSeparator ).collect { testRoot ->
            if ( testRoot ) {
                def dir = new File( testRoot )
                if ( dir.isDirectory() ) {
                    return dir
                }
            }
            null
        }.findAll { it != null }

        def className = getSpecClassName( data )

        for ( File root in existingRoots ) {
            // bug in Spock: if the package name and class name are the same, the package is just the default package
            def packageName = ''
            if ( data.info.package && data.info.package != className ) {
                packageName = data.info.package
            }

            List<String> pathParts = [ ]
            if ( packageName ) {
                pathParts += packageName.split( /\./ ).toList()
            }
            pathParts << data.info.filename

            def specFile = Paths.get( root.absolutePath, *pathParts ).toFile()

            if ( specFile.isFile() ) {
                return specFile
            }
        }

        return null
    }

    /**
     * Converts a value of any type to the given type.
     *
     * This only works for a few known Java primitive types!
     *
     * @param value
     * @param type
     * @return converted property value
     */
    @SuppressWarnings( "GroovyAssignabilityCheck" )
    static <T> T convertProperty( value, Class<T> type ) {
        if ( value == null ) {
            return null
        }

        switch ( type ) {
            case String:
                return value.toString()
            case Integer:
                return Integer.valueOf( Integer.parseInt( value as String ) )
            case int:
                return Integer.parseInt( value as String )
            case Float:
                return Float.valueOf( Float.parseFloat( value as String ) )
            case float:
                return Float.parseFloat( value as String )
            case Long:
                return Long.valueOf( Long.parseLong( value as String ) )
            case long:
                return Long.parseLong( value as String )
            case Double:
                return Double.valueOf( Double.parseDouble( value as String ) )
            case double:
                return Double.parseDouble( value as String )
            case Byte:
                return Byte.valueOf( Byte.parseByte( value as String ) )
            case byte:
                return Byte.parseByte( value as String )
            case Boolean:
                return Boolean.valueOf( Boolean.parseBoolean( value as String ) )
            case boolean:
                return Boolean.parseBoolean( value as String )
            case Character:
            case char:
                char convertedValue
                if ( value instanceof Character ) {
                    convertedValue = ( char ) value
                } else if ( value instanceof CharSequence && value.size() == 1 ) {
                    convertedValue = value as char
                } else {
                    throw new IllegalArgumentException( "Cannot convert to char: " + value )
                }
                if ( type == char ) {
                    return convertedValue
                } else {
                    return Character.valueOf( convertedValue )
                }
            default:
                throw new IllegalArgumentException( "Cannot convert to type: " + type )
        }
    }

}
