package com.athaydes.spockframework.report.internal

import org.spockframework.runtime.model.ErrorInfo
import org.spockframework.runtime.model.FeatureInfo
import org.spockframework.runtime.model.IterationInfo
import org.spockframework.runtime.model.SpecInfo

/**
 *
 * User: Renato
 */
class SpecData {
	SpecInfo info
	List<FeatureRun> featureRuns = [ ]
	long totalTime
}

class FeatureRun {
	FeatureInfo feature
	Map<IterationInfo, List<ErrorInfo>> failuresByIteration = [ : ]
	Throwable error
}
