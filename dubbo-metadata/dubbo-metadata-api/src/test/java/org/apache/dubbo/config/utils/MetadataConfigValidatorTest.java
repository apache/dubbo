package org.apache.dubbo.config.utils;

import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.validator.MetadataConfigValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MetadataConfigValidatorTest {

    @Test
    void testValidateMetadataConfig() {
        MetadataReportConfig config = new MetadataReportConfig();
        config.setAddress("protocol://ip:host");
        try {
            MetadataConfigValidator.validateMetadataConfig(config);
        } catch (Exception e) {
            Assertions.fail("valid config expected.");
        }

        config.setAddress("ip:host");
        config.setProtocol("protocol");
        try {
            MetadataConfigValidator.validateMetadataConfig(config);
        } catch (Exception e) {
            Assertions.fail("valid config expected.");
        }

        config.setAddress("ip:host");
        config.setProtocol(null);
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            MetadataConfigValidator.validateMetadataConfig(config);
        });
    }

}
