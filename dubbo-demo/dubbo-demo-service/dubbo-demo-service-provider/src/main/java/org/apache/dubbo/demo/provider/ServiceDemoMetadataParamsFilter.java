package org.apache.dubbo.demo.provider;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.metadata.MetadataParamsFilter;

/**
 * @author: hongwei.quhw
 * @date: 2020-08-05 19:33
 */
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
