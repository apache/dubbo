package org.apache.dubbo.config.deploy.lifecycle.managers.method;

import org.apache.dubbo.common.constants.SpiMethodNames;
import org.apache.dubbo.config.deploy.lifecycle.SpiMethod;

public class MockSpiMethod1 implements SpiMethod {

    @Override
    public SpiMethodNames methodName() {
        return SpiMethodNames.isSupportMonitor;
    }

    @Override
    public boolean attachToApplication() {
        return true;
    }

    @Override
    public Object invoke(Object... params) {
        return true;
    }
}
