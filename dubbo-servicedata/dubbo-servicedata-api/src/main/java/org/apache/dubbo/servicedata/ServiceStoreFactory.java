package org.apache.dubbo.servicedata;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.SPI;

/**
 * @author cvictory ON 2018/8/24
 */
@SPI("dubbo")
public interface ServiceStoreFactory {

    @Adaptive({"protocol"})
    ServiceStore getServiceStore(URL url);
}
