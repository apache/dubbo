package org.apache.dubbo.errorcode.util;

import org.apache.dubbo.errorcode.extractor.ErrorCodeExtractor;

/**
 * Utilities of generating URLs.
 */
public final class ErrorUrlUtils {
    private ErrorUrlUtils() {
        throw new UnsupportedOperationException("No instance of ErrorUrlUtils for you! ");
    }

    private static final String INSTRUCTIONS_URL = "https://dubbo.apache.org/faq/%d/%d";

    public static String getErrorUrl(String code) {

        String trimmedString = code.trim();

        if (!ErrorCodeExtractor.ERROR_CODE_PATTERN.matcher(trimmedString).matches()) {
            return "";
        }

        String[] segments = trimmedString.split("[-]");

        int[] errorCodeSegments = new int[2];

        try {
            errorCodeSegments[0] = Integer.parseInt(segments[0]);
            errorCodeSegments[1] = Integer.parseInt(segments[1]);
        } catch (NumberFormatException numberFormatException) {
            return "";
        }

        return String.format(INSTRUCTIONS_URL, errorCodeSegments[0], errorCodeSegments[1]);
    }
}
