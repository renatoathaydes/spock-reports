package com.athaydes.spockframework.report.internal

import com.athaydes.spockframework.report.IReportCreator

import javax.naming.OperationNotSupportedException


/**
 * Container for multiple IReportCreators
 */
class MultiReportCreator implements IReportCreator {
    private final List<IReportCreator> reportCreators = [ ]

    MultiReportCreator( List<IReportCreator> reportCreators ) {
        this.reportCreators.addAll( reportCreators )
    }

    @Override
    void createReportFor( SpecData data ) {
        reportCreators.each { it.createReportFor( data ) }
    }

    @Override
    void setOutputDir( String path ) {
        throw new OperationNotSupportedException( "No modifications after construction" )
    }

    @Override
    void setHideEmptyBlocks( boolean hide ) {
        throw new OperationNotSupportedException( "No modifications after construction" )
    }

    @Override
    void setShowCodeBlocks( boolean show ) {
        throw new OperationNotSupportedException( "No modifications after construction" )
    }

    @Override
    void setTestSourceRoots( String roots ) {
        throw new OperationNotSupportedException( "No modifications after construction" )
    }

    @Override
    void setProjectName( String projectName ) {
        throw new OperationNotSupportedException( "No modifications after construction" )
    }

    @Override
    void setProjectVersion( String projectVersion ) {
        throw new OperationNotSupportedException( "No modifications after construction" )
    }

    @Override
    void done() {
        reportCreators.each { it.done() }
    }
}
