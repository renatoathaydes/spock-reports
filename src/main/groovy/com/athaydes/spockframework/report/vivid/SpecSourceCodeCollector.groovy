package com.athaydes.spockframework.report.vivid

import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.Expression
import org.spockframework.compiler.SourceLookup
import org.spockframework.runtime.model.BlockKind

class SpecSourceCodeCollector {

    private static final String[] IGNORED_LABELS = ["where"]
    private static final Map<String, String> MAPPING = [given: "setup", and: null]

    private final SourceLookup sourceLookup
    private final SpecSourceCode specSourceCode = [:]
    private BlockKind lastBlockKind

    SpecSourceCodeCollector(SourceLookup sourceLookup) {
        this.sourceLookup = sourceLookup
    }

    SpecSourceCode getResult() {
        return specSourceCode
    }

    void addExpression(MethodNode feature, String label, Expression expression) {
        if (IGNORED_LABELS.contains(label)) {
            return
        }

        BlockKind blockKind = toBlockKind(label)
        if (blockKind) {
            lastBlockKind = blockKind
        }

        String sourceLine = toSourceCode(expression)
        specSourceCode.addLine(feature, lastBlockKind, sourceLine)
    }

    static BlockKind toBlockKind(String label) {
        String kind = MAPPING.get(label, label)
        return kind ? BlockKind.valueOf(kind.toUpperCase()) : null
    }

    private String toSourceCode(Expression expression) {
        String source = sourceLookup.lookup(expression)
        return isInQuotationMarks(source) ? source[1..-2] : source
    }

    private static boolean isInQuotationMarks(String source) {
        return source && source.startsWith('"') && source.endsWith('"')
    }
}
