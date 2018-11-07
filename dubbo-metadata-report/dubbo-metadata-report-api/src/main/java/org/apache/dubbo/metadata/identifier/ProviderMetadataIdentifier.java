package org.apache.dubbo.metadata.identifier;

import org.apache.dubbo.common.Constants;

/**
 * 2018/10/25
 */
public class ProviderMetadataIdentifier extends MetadataIdentifier {

    public ProviderMetadataIdentifier() {

    }

    public ProviderMetadataIdentifier(String serviceInterface, String version, String group) {
        super(serviceInterface, version, group, Constants.PROVIDER_SIDE);
    }
}
