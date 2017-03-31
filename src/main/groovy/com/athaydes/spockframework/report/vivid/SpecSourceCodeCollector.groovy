package com.athaydes.spockframework.report.vivid

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.spockframework.compiler.SourceLookup
import org.spockframework.util.Nullable

@Slf4j
@CompileStatic
class SpecSourceCodeCollector {

    private static final Set<String> IGNORED_LABELS = ( [ "where" ] as Set ).asImmutable()

    final SourceLookup sourceLookup
    private final Map<String, SpecSourceCode> specSourceCodeByClassName = [ : ]

    @Nullable
    // the current class being parsed
    private String className
    int blockIndex = -1

    SpecSourceCodeCollector( SourceLookup sourceLookup ) {
        this.sourceLookup = sourceLookup
    }

    void setClassName( String className ) {
        this.@className = className
        specSourceCodeByClassName[ className ] = new SpecSourceCode()
    }

    @Nullable
    SpecSourceCode getResultFor( String className ) {
        specSourceCodeByClassName.remove( className )
    }

    void add( MethodNode feature, Statement st ) {
        def code = sourceLookup.lookup( st )
        def label = st.statementLabel
        println "LABEL: $label -> $code"
        if ( label ) {
            blockIndex++
            if ( st instanceof ExpressionStatement ) {
                def expr = ( st as ExpressionStatement ).expression
                if ( isStringConstant( expr ) ) {
                    return
                }
            }
        }

        specSourceCodeByClassName[ className ].addLine( feature, blockIndex, code )
    }

    private static boolean isStringConstant( Expression expression ) {
        expression instanceof ConstantExpression && expression.type.name == 'java.lang.String'
    }

}
