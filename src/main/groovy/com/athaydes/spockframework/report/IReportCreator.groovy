package com.athaydes.spockframework.report

import com.athaydes.spockframework.report.internal.SpecData

/**
 * A spock-reports report creator.
 */
interface IReportCreator {

    void createReportFor( SpecData data )

    void setOutputDir( String path )

    void setAggregatedJsonReportDir( String path )

    void setHideEmptyBlocks( boolean hide )

    void setShowCodeBlocks( boolean show )

    void setTestSourceRoots( String roots )

    void setProjectName( String projectName )

    void setProjectVersion( String projectVersion )

    void done()

}