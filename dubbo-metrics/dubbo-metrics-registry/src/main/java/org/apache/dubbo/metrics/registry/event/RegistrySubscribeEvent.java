package org.apache.dubbo.metrics.registry.event;

import org.apache.dubbo.metrics.model.TimePair;
import org.apache.dubbo.rpc.model.ApplicationModel;

public class RegistrySubscribeEvent extends RegistryEvent<ApplicationModel> {

    public RegistrySubscribeEvent(ApplicationModel applicationModel, TimePair timePair) {
        super(applicationModel, timePair);
    }

}
