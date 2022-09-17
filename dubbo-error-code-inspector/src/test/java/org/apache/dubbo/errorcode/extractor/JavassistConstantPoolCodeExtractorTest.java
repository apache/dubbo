package org.apache.dubbo.errorcode.extractor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests of JavassistConstantPoolCodeExtractor.
 */
class JavassistConstantPoolCodeExtractorTest {

    private static final JavassistConstantPoolErrorCodeExtractor ERROR_CODE_EXTRACTOR = new JavassistConstantPoolErrorCodeExtractor();

    @Test
    void testGettingErrorCodes() {
        String resourceFilePath = getClass().getClassLoader().getResource("FileCacheStore.class").toString();

        if (resourceFilePath.startsWith("file:/")) {
            resourceFilePath = resourceFilePath.replace("file:/", "");
        }

        Assertions.assertTrue(ERROR_CODE_EXTRACTOR.getErrorCodes(resourceFilePath).contains("0-4"));
    }
}
