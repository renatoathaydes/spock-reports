package com.athaydes.spockframework.report.util

import java.util.regex.Pattern

final class Strings {

    private static final Pattern urlPattern

    static {
        def urlRegex = "\\(?\\b([A-z]+://|[A-z0-9]+[.])[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_()|]"
        urlPattern = Pattern.compile( urlRegex )
    }

    static boolean isUrl( String text ) {
        text ==~ urlPattern
    }

    static boolean isEmptyOrContainsOnlyEmptyStrings( List<String> strings ) {
        !strings || strings.every { String it -> it.trim() == '' }
    }
}
