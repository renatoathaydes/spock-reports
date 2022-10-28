package com.athaydes.spockframework.report.vivid;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.control.Janitor;
import org.codehaus.groovy.control.SourceUnit;

/**
 *  A modified version  of {@link org.spockframework.compiler.SourceLookup} which does not
 *  trim the indents of the source code read from provided {@link ASTNode} instances.
 */
public class CustomSourceLookup {
    private final SourceUnit sourceUnit;
    private final Janitor janitor = new Janitor();

    public CustomSourceLookup( SourceUnit sourceUnit ) { this.sourceUnit = sourceUnit; }

    public String lookup( ASTNode node ) {
        StringBuilder text = new StringBuilder();
        for ( int i = node.getLineNumber(); i <= node.getLastLineNumber(); i++ ) {
            String line = sourceUnit.getSample(i, 0, janitor);
            if ( line == null )
                return null; // most probably a Groovy bug, but we prefer to handle this situation gracefully

            try {
                if ( i == node.getLastLineNumber() ) line = line.substring(0, node.getLastColumnNumber() - 1);
                text.append( line );
                if ( i != node.getLastLineNumber() ) text.append('\n');
            } catch ( StringIndexOutOfBoundsException e ) {
                return null; // most probably a Groovy bug, but we prefer to handle this situation gracefully
            }
        }
        return text.toString();
    }
}

