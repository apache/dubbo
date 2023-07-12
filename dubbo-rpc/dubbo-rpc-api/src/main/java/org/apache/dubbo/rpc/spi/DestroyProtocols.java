package org.apache.dubbo.rpc.spi;

import org.apache.dubbo.common.constants.PackageName;
import org.apache.dubbo.common.constants.SpiMethods;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.deploy.lifecycle.SpiMethod;
import org.apache.dubbo.config.deploy.lifecycle.manager.SpiMethodManager;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_UNDEFINED_PROTOCOL;

public class DestroyProtocols implements SpiMethod {

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(DestroyProtocols.class);

    @Override
    public SpiMethods methodName() {
        return SpiMethods.destroyProtocols;
    }

    @Override
    public boolean attachToApplication() {
        return false;
    }

    @Override
    public Object invoke(Object... params) {
        destroyProtocols((FrameworkModel) params[0],(AtomicBoolean) params[1]);
        return null;
    }

    private void destroyProtocols(FrameworkModel frameworkModel, AtomicBoolean protocolDestroyed) {
        if (protocolDestroyed.compareAndSet(false, true)) {
            ExtensionLoader<Protocol> loader = frameworkModel.getExtensionLoader(Protocol.class);
            for (String protocolName : loader.getLoadedExtensions()) {
                try {
                    Protocol protocol = loader.getLoadedExtension(protocolName);
                    if (protocol != null) {
                        protocol.destroy();
                    }
                } catch (Throwable t) {
                    logger.warn(CONFIG_UNDEFINED_PROTOCOL, "", "", t.getMessage(), t);
                }
            }
        }
    }
}
