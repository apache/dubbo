package org.apache.dubbo.demo.provider;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.metadata.MetadataParamsFilter;


@Activate
public class ServiceDemoMetadataParamsFilter implements MetadataParamsFilter {
    @Override
    public String[] serviceParamsIncluded() {
        return new String[] {"serviceKey1", "serviceKey2"};
    }

    @Override
    public String[] instanceParamsIncluded() {
        return new String[] {"instance1", "instance2"};
    }
}
