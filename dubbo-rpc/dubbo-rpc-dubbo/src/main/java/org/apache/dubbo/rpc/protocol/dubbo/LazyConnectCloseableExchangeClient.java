package org.apache.dubbo.rpc.protocol.dubbo;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.exchange.ExchangeHandler;

/**
 * dubbo protocol support class.
 */
@SuppressWarnings("deprecation")
public class LazyConnectCloseableExchangeClient extends LazyConnectExchangeClient {
    public LazyConnectCloseableExchangeClient(URL url, ExchangeHandler requestHandler) {
        super(url, requestHandler);
    }
}
