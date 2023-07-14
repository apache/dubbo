package org.apache.dubbo.metrics.registry.spi;

import org.apache.dubbo.common.constants.SpiMethodNames;
import org.apache.dubbo.config.deploy.lifecycle.SpiMethod;
import org.apache.dubbo.metrics.registry.event.RegistryEvent;
import org.apache.dubbo.rpc.model.ApplicationModel;


public class ToRsEvent implements SpiMethod {

    @Override
    public SpiMethodNames methodName() {
        return SpiMethodNames.toRsEvent;
    }

    @Override
    public boolean attachToApplication() {
        return false;
    }

    /**
     * The spi method.
     *
     * @param params params
     * @return return value
     */
    @Override
    public Object invoke(Object... params) {
        return RegistryEvent.toRsEvent((ApplicationModel) params[0], (String) params[1], (Integer) params[2]);
    }
}
