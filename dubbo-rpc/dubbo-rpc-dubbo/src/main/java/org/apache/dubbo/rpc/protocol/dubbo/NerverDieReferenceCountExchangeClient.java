/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.rpc.protocol.dubbo;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.exchange.ExchangeClient;
import org.apache.dubbo.remoting.exchange.ExchangeHandler;

import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.dubbo.remoting.Constants.SEND_RECONNECT_KEY;
import static org.apache.dubbo.rpc.protocol.dubbo.Constants.LAZY_CONNECT_INITIAL_STATE_KEY;

/**
 * dubbo protocol support class.
 */
@Deprecated
final class NerverDieReferenceCountExchangeClient extends ReferenceCountExchangeClient {

    private static final Logger logger = LoggerFactory.getLogger(NerverDieReferenceCountExchangeClient.class);
    private final URL url;
    private final ExchangeHandler exchangeHandler;

    @Deprecated
    private final AtomicInteger disconnectCount = new AtomicInteger(0);
    @Deprecated
    private final Integer warningPeriod = 50;

    public NerverDieReferenceCountExchangeClient(ExchangeClient client) {
        super(client);
        this.url = client.getUrl();
        this.exchangeHandler = client.getExchangeHandler();
    }

    @Override
    protected ExchangeClient getExchangeClient() {
        ExchangeClient exchangeClient = super.getExchangeClient();
        if(exchangeClient != null){
            return exchangeClient;
        }

        synchronized(this) {
            exchangeClient = super.getExchangeClient();
            if(exchangeClient != null){
                return exchangeClient;
            }
            // this is a defensive operation to avoid client is closed by accident, the initial state of the client is false
            URL lazyUrl = url.addParameter(LAZY_CONNECT_INITIAL_STATE_KEY, Boolean.TRUE)
                    //.addParameter(RECONNECT_KEY, Boolean.FALSE)
                    .addParameter(SEND_RECONNECT_KEY, Boolean.TRUE.toString());
            //.addParameter(LazyConnectExchangeClient.REQUEST_WITH_WARNING_KEY, true);
            exchangeClient = new LazyConnectExchangeClient(lazyUrl, exchangeHandler);
            setExchangClient(exchangeClient);
        }
        return exchangeClient;
    }

    /**
     * when destroy unused invoker, closeAll should be true
     *
     * @param timeout
     * @param closeAll
     */
    @Override
    protected boolean closeInternal(int timeout, boolean closeAll) {
        boolean close = super.closeInternal(timeout, closeAll);
        if(close) {
            // start warning at second replaceWithLazyClient()
            if (disconnectCount.getAndIncrement() % warningPeriod == 1) {
                logger.warn(url.getAddress() + " " + url.getServiceKey() + " safe guard client , should not be called ,must have a bug.");
            }
        }
        return close;
    }

    @Override
    public boolean addRef() {
        refCountInc();
        return true;
    }

    /**
     * create func
     */
    public interface CreateExchangeFunc {
        ExchangeClient create(URL url);
    }
}

