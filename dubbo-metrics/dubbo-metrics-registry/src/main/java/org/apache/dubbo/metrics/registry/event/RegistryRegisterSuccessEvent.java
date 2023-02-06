package org.apache.dubbo.metrics.registry.event;

import org.apache.dubbo.rpc.model.ApplicationModel;

public class RegistryRegisterSuccessEvent extends RegistryEvent<ApplicationModel> {

    public RegistryRegisterSuccessEvent(ApplicationModel applicationModel, Type type) {
        super(applicationModel, type);
    }

}
