package com.athaydes.spockframework.report.util

import com.athaydes.spockframework.report.extension.InfoContainer
import com.athaydes.spockframework.report.internal.FailureKind
import com.athaydes.spockframework.report.internal.FeatureRun
import com.athaydes.spockframework.report.internal.SpecData
import com.athaydes.spockframework.report.internal.SpecProblem
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.spockframework.runtime.model.BlockKind
import org.spockframework.runtime.model.FeatureInfo
import org.spockframework.runtime.model.IterationInfo
import org.spockframework.runtime.model.SpecInfo
import org.spockframework.util.Nullable
import spock.lang.PendingFeature
import spock.lang.Specification

import java.lang.annotation.Annotation
import java.nio.file.Paths
import java.util.regex.Pattern

@CompileStatic
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

    @CompileStatic
    static double successRate( int total, int reproved ) {
        if ( total == 0 && reproved == 0 ) return 1.0D
        if ( total == 0 ) return 0.0D
        double passed = total - reproved
        Math.min( 1.0D, Math.max( 0.0D, ( passed / total ) as double ) )
    }

    static Map stats( SpecData data ) {
        def total = data.info.allFeatures.size()

        def failures = data.withFeatureRuns { countProblems( it, this.&isFailure ) }
        def errors = computeErrorCount( data )
        def skipped = data.info.allFeaturesInExecutionOrder.count { FeatureInfo f -> isSkipped( f ) }
        def totalExecuted = data.withFeatureRuns {
            countFeatures( it ) { FeatureRun run -> !isSkipped( run.feature ) }
        }
        def successRate = computeSuccessRate( data, totalExecuted, errors, failures )

        [ failures: failures, errors: errors,
          skipped : skipped, totalRuns: totalExecuted, totalFeatures: total,
          passed  : Math.max( 0, totalExecuted - failures - errors ), successRate: successRate, time: data.totalTime ]
    }

    private static int computeErrorCount( SpecData data ) {
        if ( data.initializationError ) return 1
        def executionErrors = data.withFeatureRuns { countProblems( it, this.&isError ) }
        if ( data.cleanupSpecError && executionErrors == 0 ) {
            // at least one error is needed to mark the spec as having failed due to the cleanupSpec error
            return 1
        }
        return executionErrors
    }

    private static double computeSuccessRate( SpecData data, int totalExecuted, int errors, int failures ) {
        if ( data.initializationError || data.cleanupSpecError ) return 0.0D
        if ( totalExecuted == 0 ) return 1.0D
        successRate( totalExecuted, ( errors + failures ).toInteger() )
    }

    @CompileDynamic
    static Map aggregateStats( Map<String, Map> aggregatedData ) {
        def result = [ total : 0, executed: 0, passed: 0, failed: 0, skipped: 0,
                       fTotal: 0, fExecuted: 0, fPassed: 0, fSkipped: 0, fFails: 0, fErrors: 0, time: 0.0 ]
        aggregatedData.values().each { Map json ->
            def stats = json.stats
            def isFailure = stats.failures + stats.errors > 0
            def isSkipped = stats.totalRuns == 0
            result.total += 1
            result.executed += ( isSkipped ? 0 : 1 )
            result.passed += isSkipped ? 0 : ( isFailure ? 0 : 1 )
            result.failed += ( isFailure ? 1 : 0 )
            result.skipped += isSkipped ? 1 : 0
            result.fTotal += stats.totalFeatures
            result.fExecuted += stats.totalRuns
            result.fFails += stats.failures
            result.fErrors += stats.errors
            result.fSkipped += stats.skipped
            result.fPassed += stats.passed
            result.time += stats.time
        }
        result.successRate = successRate( result.executed, result.failed )
        result.fSuccessRate = successRate( result.fExecuted, result.fFails + result.fErrors )
        result
    }

    static boolean isEmptyOrContainsOnlyEmptyStrings( List<String> strings ) {
        !strings || strings.every { String it -> it.trim() == '' }
    }

    static boolean isUnrolled( FeatureInfo feature ) {
        feature.reportIterations
    }

    static boolean isFailure( SpecProblem problem ) {
        problem.kind == FailureKind.FAILURE
    }

    static boolean isError( SpecProblem problem ) {
        problem.kind == FailureKind.ERROR
    }

    static boolean isSkipped( FeatureInfo featureInfo ) {
        // pending features are not marked as skipped but they are always skipped or fail
        featureInfo.skipped || featureAnnotation( featureInfo, PendingFeature ) != null
    }

    static int countFeatures( List<FeatureRun> runs,
                              Closure<Boolean> featureFilter = { _ -> true } ) {
        runs.findAll( featureFilter ).inject( 0 ) { int count, FeatureRun fr ->
            count + ( isUnrolled( fr.feature ) ? fr.iterationCount() : 1 )
        } as int
    }

    static int countProblems( List<FeatureRun> runs, Closure problemFilter ) {
        runs.inject( 0 ) { int count, FeatureRun fr ->
            // count how many iterations had one or more failures... a single iteration may
            // fail many times!
            def allProblems = fr.copyFailuresByIteration().values().count { iterationFailures ->
                iterationFailures.any( problemFilter )
            }
            count + ( allProblems > 0
                    ? // only count 1 feature failure if NOT unrolled
                    ( isUnrolled( fr.feature ) ? allProblems : 1 )
                    : 0 )
        } as int
    }

    static String iterationsResult( FeatureRun run ) {
        def totalErrors = run.copyFailuresByIteration().values().count { List it -> !it.empty }
        "${run.iterationCount() - totalErrors}/${run.iterationCount()} passed"
    }

    static List<Map> iterationData( Map<IterationInfo, List<SpecProblem>> failures, Map<IterationInfo, Long> times ) {
        failures.inject( [ ] ) { List<Map> acc, IterationInfo iteration, List<SpecProblem> failureList ->
            def allErrors = failureList.collect { SpecProblem it -> it.failure.exception }
            def time = times.get( iteration, 0L )
            acc << [ dataValues: iteration.dataValues, errors: allErrors, time: time, info: iteration ]
        }
    }

    static <A extends Annotation> A specAnnotation( SpecData data, Class<A> annotation ) {
        data.info.isAnnotationPresent( annotation )
                ? data.info.getAnnotation( annotation ) as A
                : null
    }

    static <A extends Annotation> A featureAnnotation( FeatureInfo feature, Class<A> annotation ) {
        feature.featureMethod.isAnnotationPresent( annotation )
                ? feature.featureMethod.getAnnotation( annotation ) as A
                : null
    }

    static List nextSpecExtraInfo( SpecData data ) {
        throw new UnsupportedOperationException( "This method has been removed in spock-reports 2.2.0-groovy-3.0. " +
                "If you are calling this method from a template report, please replace your " +
                "`utils.nextSpecExtraInfo( data )` invocation with " +
                "`utils.nextSpecExtraInfo( data, feature, iteration )`. For more details, see " +
                "https://github.com/renatoathaydes/spock-reports/issues/219" )
    }

    static List nextSpecExtraInfo( SpecData data,
                                   FeatureInfo feature,
                                   IterationInfo iteration = null ) {
        InfoContainer.getNextInfoFor( getSpecClassName( data ), feature, iteration )
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

    static String featureNameFrom( FeatureInfo feature, IterationInfo iteration,
                                   int index, boolean multipleIterations = false ) {
        if ( feature.reportIterations && multipleIterations ) {
            if ( feature.name.contains( '#' ) && feature.iterationNameProvider ) {
                def name = feature.iterationNameProvider.getName( iteration )
                return "$name [$index]"
            }
            return "${feature.name} [$index]"
        }
        return feature.name
    }

    static String getSpecClassName( SpecData data ) {
        getSpecClassName( data.info )
    }

    static String getSpecClassName( SpecInfo info ) {
        specNameFromFileName( info )
    }

    static List<String> getParentSpecNames( String className ) {
        List<String> result = new ArrayList<>( 2 )
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

        return ( specInfo.package ? specInfo.package + '.' : '' ) + name
    }

    private static void collectRoots( root, List<String> result ) {
        switch ( root ) {
            case File:
                result.add( ( ( File ) root ).path )
                break
            case String:
                result.add( ( String ) root )
                break
            case Iterable:
                ( root as Iterable ).each { r -> collectRoots( r, result ) }
                break
            case Closure:
                collectRoots( ( root as Closure ).call(), result )
                break
            default:
                throw new IllegalArgumentException( "Cannot use object as a sourceRoot " +
                        "(acceptable types are String, File, Iterables of those, Closures providing those): $root" )
        }
    }

    @Nullable
    static File getSpecFile( Object testSourceRoots, SpecData data ) {
        List<String> roots = [ ]
        collectRoots( testSourceRoots, roots )
        getSpecFile( roots, data )
    }

    @Nullable
    static File getSpecFile( List<String> testSourceRoots, SpecData data ) {
        List<File> existingRoots = testSourceRoots.collectMany {
            it.split( File.pathSeparator ).collect { testRoot ->
                if ( testRoot ) {
                    def dir = new File( testRoot )
                    if ( dir.isDirectory() ) {
                        return dir
                    }
                }
                null
            }.findAll { it != null }
        }

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

            def specFile = Paths.get( root.absolutePath, pathParts.toArray( new String[0] ) ).toFile()

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
                // fallthrough
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
            case Object:
                return value
            default:
                throw new IllegalArgumentException( "Cannot convert to type " + type.name + ": " + value )
        }
    }

}
