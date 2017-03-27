package com.athaydes.spockframework.report.vivid

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.Expression

@CompileStatic
class VividVisitCallback {

    SpecSourceCodeCollector codeCollector

    // temp storage for method currently being visited
    private final Queue<MethodNode> methodsVisits = [ ] as Queue

    void startClass( String className ) {
        codeCollector.className = className
    }

    void onMethodEntry( MethodNode methodNode ) {
        methodsVisits.add( methodNode )
    }

    void onMethodExit() {
        methodsVisits.poll()
    }

    void addExpressionNode( String label, Expression expression ) {
        MethodNode currentMethod = methodsVisits.peek()
        codeCollector.addExpression( currentMethod, label, expression )
    }

}
