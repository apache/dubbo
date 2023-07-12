package org.apache.dubbo.rpc.protocol.injvm.spi;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.SpiMethods;
import org.apache.dubbo.config.deploy.lifecycle.SpiMethod;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.protocol.injvm.InjvmProtocol;

/**
 * {@link InjvmProtocol#isInjvmRefer(URL)}
 */
public class IsJvmRefer implements SpiMethod {

    @Override
    public SpiMethods methodName() {
        return SpiMethods.isJvmRefer;
    }

    @Override
    public boolean attachToApplication() {
        return false;
    }

    @Override
    public Object invoke(Object... params) {

        ModuleModel moduleModel = (ModuleModel) params[0];
        URL tmpUrl = (URL) params[1];

        return InjvmProtocol.getInjvmProtocol(moduleModel).isInjvmRefer(tmpUrl);
    }
}
