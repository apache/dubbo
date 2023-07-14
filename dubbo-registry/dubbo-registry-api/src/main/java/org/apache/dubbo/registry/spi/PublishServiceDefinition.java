package org.apache.dubbo.registry.spi;


import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.SpiMethodNames;
import org.apache.dubbo.config.deploy.lifecycle.SpiMethod;
import org.apache.dubbo.registry.client.metadata.MetadataUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;

public class PublishServiceDefinition implements SpiMethod {

    @Override
    public SpiMethodNames methodName() {
        return SpiMethodNames.publishServiceDefinition;
    }

    @Override
    public boolean attachToApplication() {
        return false;
    }

    /**
     * {@link MetadataUtils#publishServiceDefinition(URL, ServiceDescriptor, ApplicationModel)}
     *
     * @param params params
     * @return return value
     */
    @Override
    public Object invoke(Object... params) {
        MetadataUtils.publishServiceDefinition((URL)params[0],(ServiceDescriptor) params[1],(ApplicationModel) params[2]);
        return null;
    }
}

