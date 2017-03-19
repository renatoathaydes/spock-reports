package com.athaydes.spockframework.report.vivid

import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.Expression

class VividVisitCallback {

    private final Queue<MethodNode> methodsVisits = [] as Queue

    SpecSourceCodeCollector codeCollector

    void onMethodEntry(MethodNode methodNode) {
        methodsVisits.add(methodNode)
    }

    void onMethodExit() {
        methodsVisits.poll()
    }

    void addExpressionNode(String label, Expression expression) {
        MethodNode currentMethod = methodsVisits.peek()
        codeCollector.addExpression(currentMethod, label, expression)
    }

}
