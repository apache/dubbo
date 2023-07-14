package org.apache.dubbo.config.deploy.lifecycle;

import org.apache.dubbo.common.constants.SpiMethodNames;
import org.apache.dubbo.common.extension.SPI;

/**
 * Spi method.
 */
@SPI
public interface SpiMethod {

    /**
     * The name of this method.
     * @return the name of this method
     */
    SpiMethodNames methodName();

    /**
     * Whether this method binding to a certain {@link org.apache.dubbo.rpc.model.ApplicationModel}.
     * <br>
     * If not, this SpiMethod can invoke directly by {@link org.apache.dubbo.config.deploy.lifecycle.manager.SpiMethodManager}
     * without providing any application name.
     *
     * @return Whether this method binding to a certain {@link org.apache.dubbo.rpc.model.ApplicationModel}.
     */
    boolean attachToApplication();

    /**
     * The spi method.
     * @param params params
     * @return return value
     */
    Object invoke(Object... params);
}
