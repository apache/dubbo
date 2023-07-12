package org.apache.dubbo.monitor.spi;

import org.apache.dubbo.common.constants.PackageName;
import org.apache.dubbo.common.constants.SpiMethods;
import org.apache.dubbo.config.AbstractInterfaceConfig;
import org.apache.dubbo.config.deploy.lifecycle.SpiMethod;
import org.apache.dubbo.monitor.MonitorFactory;

import static org.apache.dubbo.common.constants.MonitorConstants.LOGSTAT_PROTOCOL;

public class IsSupportMonitor implements SpiMethod {

    @Override
    public SpiMethods methodName() {
        return SpiMethods.isSupportMonitor;
    }

    @Override
    public boolean attachToApplication() {
        return false;
    }

    /**
     * Whether support monitor.
     *
     * @param params params
     * @return return value
     */
    @Override
    public Object invoke(Object... params) {
        AbstractInterfaceConfig config = (AbstractInterfaceConfig) params[0];
        return config.getScopeModel().getExtensionLoader(MonitorFactory.class).hasExtension(LOGSTAT_PROTOCOL);
    }
}
