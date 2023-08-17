package org.apache.dubbo.config.utils.validator;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.context.ConfigValidator;

import static org.apache.dubbo.common.utils.StringUtils.isEmpty;

@Activate
public class MetadataConfigValidator implements ConfigValidator<MetadataReportConfig> {

   @Override
    public void validate(MetadataReportConfig config) {
       validateMetadataConfig(config);
    }

    public static void validateMetadataConfig(MetadataReportConfig metadataReportConfig) {
        if (!isValidMetadataConfig(metadataReportConfig)) {
            return;
        }

        String address = metadataReportConfig.getAddress();
        String protocol = metadataReportConfig.getProtocol();

        if ((isEmpty(address) || !address.contains("://")) && isEmpty(protocol)) {
            throw new IllegalArgumentException("Please specify valid protocol or address for metadata report " + address);
        }
    }

    public static boolean isValidMetadataConfig(MetadataReportConfig metadataReportConfig) {
        if (metadataReportConfig == null) {
            return false;
        }

        if (Boolean.FALSE.equals(metadataReportConfig.getReportMetadata()) &&
            Boolean.FALSE.equals(metadataReportConfig.getReportDefinition())) {
            return false;
        }

        return !isEmpty(metadataReportConfig.getAddress());
    }



    @Override
    public boolean isSupport(Class<?> configClass) {
        return MetadataReportConfig.class.isAssignableFrom(configClass);
    }
}
