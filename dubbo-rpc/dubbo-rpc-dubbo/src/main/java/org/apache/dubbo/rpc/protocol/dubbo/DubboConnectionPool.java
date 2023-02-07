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

import static org.apache.dubbo.common.constants.CommonConstants.LAZY_CONNECT_KEY;
import static org.apache.dubbo.remoting.Constants.CLIENT_KEY;
import static org.apache.dubbo.remoting.Constants.CODEC_KEY;
import static org.apache.dubbo.remoting.Constants.DEFAULT_HEARTBEAT;
import static org.apache.dubbo.remoting.Constants.DEFAULT_REMOTING_CLIENT;
import static org.apache.dubbo.remoting.Constants.HEARTBEAT_KEY;
import static org.apache.dubbo.remoting.Constants.SERVER_KEY;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.Transporter;
import org.apache.dubbo.remoting.api.connection.pool.AbstractConnectionPool;
import org.apache.dubbo.remoting.exchange.ExchangeClient;
import org.apache.dubbo.remoting.exchange.ExchangeHandler;
import org.apache.dubbo.remoting.exchange.Exchangers;
import org.apache.dubbo.rpc.RpcException;

public class DubboConnectionPool extends AbstractConnectionPool<ExchangeClient> {

    private final Map<String, Object> referenceClientMap = new ConcurrentHashMap<>();

    private static final Object PENDING_OBJECT = new Object();

    private final ExchangeHandler requestHandler;

    public DubboConnectionPool(URL url, ExchangeHandler requestHandler) {
        super(url);
        this.requestHandler=requestHandler;
    }


    @Override
    protected ExchangeClient initConnection(URL url) {
        return getSharedClient(url);
    }

    @Override
    protected boolean isConnectionAvailable(ExchangeClient exchangeClient) {
        return exchangeClient.isConnected() //cannot write == not Available ?
            && !exchangeClient.hasAttribute(Constants.CHANNEL_ATTRIBUTE_READONLY_KEY);
    }

    @Override
    protected void closeConnection(ExchangeClient exchangeClient) {
        exchangeClient.close();
    }

    @Override
    protected void closeConnection(ExchangeClient exchangeClient, int seconds) {
        exchangeClient.close(seconds);
    }


    private ReferenceCountExchangeClient getSharedClient(URL url) {
        String key = url.getAddress();

        Object client = referenceClientMap.get(key);
        if (client instanceof ReferenceCountExchangeClient) {
            ReferenceCountExchangeClient referenceCountExchangeClient = (ReferenceCountExchangeClient) client;
            if (checkClientCanUse(referenceCountExchangeClient)) {
                referenceCountExchangeClient.incrementAndGetCount();
                return referenceCountExchangeClient;
            }
        }

        ReferenceCountExchangeClient typedClient = null;

        synchronized (referenceClientMap) {
            for (; ; ) {
                // guarantee just one thread in loading condition. And Other is waiting It had finished.
                client = referenceClientMap.get(key);

                if (client instanceof ReferenceCountExchangeClient) {
                    typedClient = (ReferenceCountExchangeClient) client;
                    if (checkClientCanUse(typedClient)) {
                        typedClient.incrementAndGetCount();
                        return typedClient;
                    } else {
                        referenceClientMap.put(key, PENDING_OBJECT);
                        break;
                    }
                } else if (client == PENDING_OBJECT) {
                    try {
                        referenceClientMap.wait();
                    } catch (InterruptedException ignored) {
                    }
                } else {
                    referenceClientMap.put(key, PENDING_OBJECT);
                    break;
                }
            }
        }

        try {

            if (typedClient == null) {
                typedClient = buildReferenceCountExchangeClient(url);
            } else {
                typedClient.incrementAndGetCount();
            }
        } finally {
            synchronized (referenceClientMap) {
                if (typedClient == null) {
                    referenceClientMap.remove(key);
                } else {
                    referenceClientMap.put(key, typedClient);
                }
                referenceClientMap.notifyAll();
            }
        }
        return typedClient;
    }

    private static boolean checkClientCanUse(
        ReferenceCountExchangeClient referenceCountExchangeClient) {
        return referenceCountExchangeClient != null
            && referenceCountExchangeClient.getCount() > 0
            && !referenceCountExchangeClient.isClosed();
    }

    private ReferenceCountExchangeClient buildReferenceCountExchangeClient(URL url) {
        ExchangeClient exchangeClient = initClient(url);
        ReferenceCountExchangeClient client = new ReferenceCountExchangeClient(exchangeClient, DubboCodec.NAME);
        // read configs
        int shutdownTimeout = ConfigurationUtils.getServerShutdownTimeout(url.getScopeModel());
        client.setShutdownWaitTime(shutdownTimeout);
        return client;
    }

    private ExchangeClient initClient(URL url) {
        /*
         * Instance of url is InstanceAddressURL, so addParameter actually adds parameters into ServiceInstance,
         * which means params are shared among different services. Since client is shared among services this is currently not a problem.
         */
        String str = url.getParameter(CLIENT_KEY, url.getParameter(SERVER_KEY, DEFAULT_REMOTING_CLIENT));

        // BIO is not allowed since it has severe performance issue.
        if (StringUtils.isNotEmpty(str) && !url.getOrDefaultFrameworkModel().getExtensionLoader(
            Transporter.class).hasExtension(str)) {
            throw new RpcException("Unsupported client type: " + str + "," +
                " supported client type is " + StringUtils.join(url.getOrDefaultFrameworkModel().getExtensionLoader(Transporter.class).getSupportedExtensions(), " "));
        }

        try {
            // Replace InstanceAddressURL with ServiceConfigURL.
            url = new ServiceConfigURL(DubboCodec.NAME, url.getUsername(), url.getPassword(), url.getHost(), url.getPort(), url.getPath(), url.getAllParameters());
            url = url.addParameter(CODEC_KEY, DubboCodec.NAME);
            // enable heartbeat by default
            url = url.addParameterIfAbsent(HEARTBEAT_KEY, String.valueOf(DEFAULT_HEARTBEAT));

            // connection should be lazy
            return url.getParameter(LAZY_CONNECT_KEY, false)
                ? new LazyConnectExchangeClient(url, requestHandler)
                : Exchangers.connect(url, requestHandler);
        } catch (RemotingException e) {
            throw new RpcException("Fail to create remoting client for service(" + url + "): " + e.getMessage(), e);
        }
    }


}
