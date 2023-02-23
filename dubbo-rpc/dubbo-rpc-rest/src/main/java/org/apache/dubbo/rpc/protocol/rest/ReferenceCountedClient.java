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
package org.apache.dubbo.rpc.protocol.rest;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.reference.ReferenceCountedResource;
import org.apache.dubbo.remoting.http.RestClient;
import org.apache.dubbo.remoting.http.factory.RestClientFactory;

import java.util.concurrent.ConcurrentMap;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_ERROR_CLOSE_CLIENT;

public class ReferenceCountedClient<T extends RestClient> extends ReferenceCountedResource {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(ReferenceCountedClient.class);

    private ConcurrentMap<String, ReferenceCountedClient<? extends RestClient>> clients;
    private URL url;
    private RestClientFactory clientFactory;

    private T client;

    public ReferenceCountedClient(T client, ConcurrentMap<String, ReferenceCountedClient<? extends RestClient>> clients, RestClientFactory clientFactory, URL url) {
        this.client = client;
        this.clients = clients;
        this.clientFactory = clientFactory;
        this.url = url;
    }

    public T getClient() {

        // for client destroy and create right now, only  lock current client
        synchronized (this) {
            ReferenceCountedClient<? extends RestClient> referenceCountedClient = clients.get(url.getAddress());

            // for double check
            if (referenceCountedClient.isDestroyed()) {
                synchronized (this) {
                    referenceCountedClient = clients.get(url.getAddress());
                    if (referenceCountedClient.isDestroyed()) {
                        RestClient restClient = clientFactory.createRestClient(url);
                        clients.put(url.getAddress(), new ReferenceCountedClient(restClient, clients, clientFactory, url));
                        return (T) restClient;
                    } else {
                        return (T) referenceCountedClient.client;
                    }
                }

            } else {
                return client;
            }
        }
    }

    public boolean isDestroyed() {
        return client.isClosed();
    }

    @Override
    protected void destroy() {
        try {
            client.close();
        } catch (Exception e) {
            logger.error(PROTOCOL_ERROR_CLOSE_CLIENT, "", "", "Close resteasy client error", e);
        }
    }
}
