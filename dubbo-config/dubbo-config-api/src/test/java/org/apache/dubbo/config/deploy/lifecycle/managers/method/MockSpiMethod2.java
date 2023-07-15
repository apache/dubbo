package org.apache.dubbo.config.deploy.lifecycle.managers.method;

import org.apache.dubbo.common.constants.SpiMethodNames;
import org.apache.dubbo.config.deploy.lifecycle.SpiMethod;

public class MockSpiMethod2 implements SpiMethod {

    @Override
    public SpiMethodNames methodName() {
        return SpiMethodNames.toRsEvent;
    }

    @Override
    public boolean attachToApplication() {
        return false;
    }

    @Override
    public Object invoke(Object... params) {
        return true;
    }


}
