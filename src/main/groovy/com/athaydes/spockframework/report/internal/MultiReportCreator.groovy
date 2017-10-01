package com.athaydes.spockframework.report.internal

import com.athaydes.spockframework.report.IReportCreator


/**
 * Container for multiple IReportCreators
 */
class MultiReportCreator implements IReportCreator {
    private final List<IReportCreator> reportCreators = []

    MultiReportCreator(List<IReportCreator> reportCreators) {
        this.reportCreators.addAll(reportCreators)
    }

    @Override
    void createReportFor(SpecData data) {
        reportCreators.each {it.createReportFor(data)}
    }

    @Override
    void setOutputDir(String path) {
        reportCreators.each {it.setOutputDir(path)}
    }

    @Override
    void setHideEmptyBlocks(boolean hide) {
        reportCreators.each {it.setHideEmptyBlocks(hide)}
    }

    @Override
    void setShowCodeBlocks(boolean show) {
        reportCreators.each {it.setShowCodeBlocks(show)}
    }

    @Override
    void setTestSourceRoots(String roots) {
        reportCreators.each {it.setTestSourceRoots(roots)}
    }

    @Override
    void setProjectName(String projectName) {
        reportCreators.each {it.setProjectName(projectName)}
    }

    @Override
    void setProjectVersion(String projectVersion) {
        reportCreators.each {it.setProjectVersion(projectVersion)}
    }

    @Override
    void done() {
        reportCreators.each {it.done()}
    }
}
