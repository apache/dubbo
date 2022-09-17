package org.apache.dubbo.errorcode.extractor;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Error code extractor interface.
 */
public interface ErrorCodeExtractor {

    Pattern ERROR_CODE_PATTERN = Pattern.compile("\\d+-\\d+");

    List<String> getErrorCodes(String classFilePath);
}
