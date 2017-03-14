package com.athaydes.spockframework.report.vivid

import com.athaydes.spockframework.report.internal.SpecData
import com.athaydes.spockframework.report.util.Utils
import groovy.util.logging.Slf4j
import org.spockframework.runtime.model.BlockInfo
import org.spockframework.runtime.model.FeatureInfo

@Slf4j
class SpecSourceCodeReader {

    private SpecSourceCode specSourceCode

    void read(SpecData data) {
        try {
            VividAstInspector inspector = new VividAstInspector()
            File file = Utils.getSpecFile(data)
            inspector.load(file)
            specSourceCode = inspector.visitCallback.codeCollector.result
        } catch (Exception e) {
            log.error("Cannot create SpecSourceCode: $e.message", e)
            specSourceCode = new SpecSourceCode()
        }
    }

    List<String> getLines(FeatureInfo feature, BlockInfo block) {
        return specSourceCode.getLines(feature.name, block.kind)
    }
}
