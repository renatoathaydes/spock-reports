package com.athaydes.spockframework.report.vivid

import com.athaydes.spockframework.report.util.Utils
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.ModuleNode
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.Janitor
import org.codehaus.groovy.control.SourceUnit
import org.spockframework.util.Nullable

import java.util.concurrent.ConcurrentHashMap

@Slf4j
@CompileStatic
class SpecSourceCodeCollector implements  AutoCloseable {

    private final SourceUnit sourceUnit
    private final Janitor janitor = new Janitor()

    final ModuleNode module

    private static final Map<String, SpecSourceCode> specSourceCodeByClassName = [ : ] as ConcurrentHashMap

    @Nullable
    private String className
    @Nullable
    private MethodNode method

    SpecSourceCodeCollector( SourceUnit sourceUnit ) {
        this.sourceUnit = sourceUnit
        this.module = sourceUnit.AST
    }

    void setClassName( String className ) {
        log.debug( "Collecting code for class {}", className )
        this.@className = className
        specSourceCodeByClassName[ className ] = new SpecSourceCode()
    }

    void setMethod( MethodNode methodNode ) {
        this.@method = methodNode
    }

    @Nullable
    SpecSourceCode getResultFor( String className ) {
        def sourceCode = specSourceCodeByClassName[ className ]
        if ( sourceCode ) {
            def parentSpecs = Utils.getParentSpecNames( className )
            for ( parentSpec in parentSpecs ) {
                def parentCode = specSourceCodeByClassName[ parentSpec ]
                if ( parentCode ) {
                    sourceCode.addParent( parentCode )
                }
            }
        }
        sourceCode
    }

    void add( Statement statement ) {
        assert className && method

        def label = statement.statementLabel
        def code = lookupCode( statement )
        def specCode = specSourceCodeByClassName[ className ]

        if ( !specCode ) {
            log.warn( "Ignoring statement, class has not been initialized: $className" )
            return
        }

        log.debug( "LABEL: $label -> $code" )
        if ( label ) {
            def labelText = stringConstant( statement )
            specCode.startBlock( method, label, labelText )

            if ( labelText ) {
                return // don't add the text to the code
            }
        }
        // All statements must be added to the code in the report
        specCode.addStatement( method, code, statement.lineNumber )
    }

    private String lookupCode( Statement statement) {
        def text = new StringBuilder()
        for (int i = statement.getLineNumber(); i <= statement.getLastLineNumber(); i++) {
            def line = sourceUnit.getSample( i, 0, janitor )
            text.append( line ).append( '\n' )
        }
        text.toString()
    }

    @Nullable
    private static String stringConstant( Statement statement ) {
        if ( statement instanceof ExpressionStatement ) {
            def expr = ( statement as ExpressionStatement ).expression
            if ( expr instanceof ConstantExpression && expr.type.name == 'java.lang.String' ) {
                return ( expr as ConstantExpression ).value as String
            }
        }

        null
    }

    @Override
    void close() {
        janitor.cleanup()
    }
}
