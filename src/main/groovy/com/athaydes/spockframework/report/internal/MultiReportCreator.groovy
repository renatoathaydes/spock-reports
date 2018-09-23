package com.athaydes.spockframework.report.internal

import com.athaydes.spockframework.report.IReportCreator
import com.athaydes.spockframework.report.extension.InfoContainer

import javax.naming.OperationNotSupportedException

import static com.athaydes.spockframework.report.util.Utils.getSpecClassName

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
        def specName = getSpecClassName( data )
        def headers = InfoContainer.getHeadersFor( specName ).asImmutable()
        def extraInfo = InfoContainer.getAllExtraInfoFor( specName ).asImmutable()
        reportCreators.each {
            InfoContainer.resetSpecData( specName,
                    headers.collect(),
                    extraInfo.collect() )

            it.createReportFor( data )
        }
    }

    @Override
    void setOutputDir( String path ) {
        throw new OperationNotSupportedException( "No modifications after construction" )
    }

    @Override
    void setAggregatedJsonReportDir( String path ) {
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
