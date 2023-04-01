package org.apache.dubbo.metrics.registry.event.support;

import org.apache.dubbo.metrics.model.MetricsLevel;
import org.apache.dubbo.metrics.model.TypeWrapper;
import org.apache.dubbo.metrics.registry.event.RegistryEvent;
import org.apache.dubbo.metrics.registry.event.type.ApplicationType;
import org.apache.dubbo.rpc.model.ApplicationModel;

import static org.apache.dubbo.metrics.registry.RegistryConstants.ATTACHMENT_KEY_DIR_NUM;

public class DirectorSupport {

    public static RegistryEvent disable(ApplicationModel applicationModel) {
        return new RegistryEvent(applicationModel, new TypeWrapper(MetricsLevel.APP, ApplicationType.D_DISABLE, null, null));
    }

    public static RegistryEvent valid(ApplicationModel applicationModel) {
        return new RegistryEvent(applicationModel, new TypeWrapper(MetricsLevel.APP, ApplicationType.D_VALID, null, null));
    }

    public static RegistryEvent unValid(ApplicationModel applicationModel) {
        return new RegistryEvent(applicationModel, new TypeWrapper(MetricsLevel.APP, ApplicationType.D_UN_VALID, null, null));
    }

    public static RegistryEvent current(ApplicationModel applicationModel, int num) {
        RegistryEvent ddEvent = new RegistryEvent(applicationModel, new TypeWrapper(MetricsLevel.APP, ApplicationType.D_CURRENT, null, null));
        ddEvent.putAttachment(ATTACHMENT_KEY_DIR_NUM, num);
        return ddEvent;
    }

    public static RegistryEvent recover(ApplicationModel applicationModel) {
        return new RegistryEvent(applicationModel, new TypeWrapper(MetricsLevel.APP, ApplicationType.D_RECOVER_DISABLE, null, null));
    }
}
