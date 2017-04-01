package com.athaydes.spockframework.report.vivid

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.ModuleNode
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.SourceUnit
import org.spockframework.compiler.SourceLookup
import org.spockframework.util.Nullable

import java.util.concurrent.ConcurrentHashMap

@Slf4j
@CompileStatic
class SpecSourceCodeCollector {

    final SourceLookup sourceLookup
    final ModuleNode module

    private final Map<String, SpecSourceCode> specSourceCodeByClassName = [ : ] as ConcurrentHashMap

    @Nullable
    private String className
    @Nullable
    private MethodNode method

    SpecSourceCodeCollector( SourceUnit sourceUnit ) {
        this.sourceLookup = new SourceLookup( sourceUnit )
        this.module = sourceUnit.AST
    }

    void setClassName( String className ) {
        this.@className = className
        specSourceCodeByClassName[ className ] = new SpecSourceCode()
    }

    void setMethod( MethodNode methodNode ) {
        this.@method = methodNode
    }

    @Nullable
    SpecSourceCode getResultFor( String className ) {
        specSourceCodeByClassName.remove( className )
    }

    void add( Statement st ) {
        assert className && method

        def code = sourceLookup.lookup( st )
        def label = st.statementLabel
        def specCode = specSourceCodeByClassName[ className ]

        if ( !specCode ) {
            log.warn( "Ignoring statement, class has not been initialized: $className" )
            return
        }

        println "LABEL: $label -> $code"
        if ( label ) {
            specCode.startBlock( method )

            if ( st instanceof ExpressionStatement ) {
                def expr = ( st as ExpressionStatement ).expression
                if ( isStringConstant( expr ) ) {
                    return
                }
            }
        }

        specCode.addStatement( method, code )
    }

    private static boolean isStringConstant( Expression expression ) {
        expression instanceof ConstantExpression && expression.type.name == 'java.lang.String'
    }

}
