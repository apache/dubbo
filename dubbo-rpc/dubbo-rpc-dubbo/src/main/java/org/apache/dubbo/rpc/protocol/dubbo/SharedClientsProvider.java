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

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;

import java.util.List;
import java.util.Objects;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_ERROR_CLOSE_CLIENT;

public class SharedClientsProvider implements ClientsProvider {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(SharedClientsProvider.class);
    private final DubboProtocol dubboProtocol;
    private final String addressKey;
    private final List<ReferenceCountExchangeClient> clients;

    public SharedClientsProvider(DubboProtocol dubboProtocol, String addressKey, List<ReferenceCountExchangeClient> clients) {
        this.dubboProtocol = dubboProtocol;
        this.addressKey = addressKey;
        this.clients = clients;
    }

    @Override
    public List<ReferenceCountExchangeClient> getClients() {
        return clients;
    }

    public synchronized boolean increaseCount() {
        if (checkClientCanUse(clients)) {
            batchClientRefIncr(clients);
            return true;
        }
        return false;
    }

    @Override
    public synchronized void close(int timeout) {
        for (ReferenceCountExchangeClient client : clients) {
            try {
                client.close(timeout);
            } catch (Throwable t) {
                logger.warn(PROTOCOL_ERROR_CLOSE_CLIENT, "", "", t.getMessage(), t);
            }
        }
        if (!checkClientCanUse(clients)) {
            dubboProtocol.scheduleRemoveSharedClient(addressKey, this);
        }
    }

    public synchronized void forceClose() {
        for (ReferenceCountExchangeClient client : clients) {
            closeReferenceCountExchangeClient(client);
        }
    }

    /**
     * Check if the client list is all available
     *
     * @param referenceCountExchangeClients
     * @return true-available，false-unavailable
     */
    private boolean checkClientCanUse(List<ReferenceCountExchangeClient> referenceCountExchangeClients) {
        if (CollectionUtils.isEmpty(referenceCountExchangeClients)) {
            return false;
        }

        // As long as one client is not available, you need to replace the unavailable client with the available one.
        return referenceCountExchangeClients.stream()
            .noneMatch(referenceCountExchangeClient -> referenceCountExchangeClient == null
                || referenceCountExchangeClient.getCount() <= 0 || referenceCountExchangeClient.isClosed());
    }

    /**
     * Increase the reference Count if we create new invoker shares same connection, the connection will be closed without any reference.
     *
     * @param referenceCountExchangeClients
     */
    private void batchClientRefIncr(List<ReferenceCountExchangeClient> referenceCountExchangeClients) {
        if (CollectionUtils.isEmpty(referenceCountExchangeClients)) {
            return;
        }
        referenceCountExchangeClients.stream()
            .filter(Objects::nonNull)
            .forEach(ReferenceCountExchangeClient::incrementAndGetCount);
    }

    /**
     * close ReferenceCountExchangeClient
     *
     * @param client
     */
    private void closeReferenceCountExchangeClient(ReferenceCountExchangeClient client) {
        if (client == null) {
            return;
        }

        try {
            if (logger.isInfoEnabled()) {
                logger.info("Close dubbo connect: " + client.getLocalAddress() + "-->" + client.getRemoteAddress());
            }

            client.close(client.getShutdownWaitTime());

            // TODO
            /*
             * At this time, ReferenceCountExchangeClient#client has been replaced with LazyConnectExchangeClient.
             * Do you need to call client.close again to ensure that LazyConnectExchangeClient is also closed?
             */

        } catch (Throwable t) {
            logger.warn(PROTOCOL_ERROR_CLOSE_CLIENT, "", "", t.getMessage(), t);
        }
    }

}
