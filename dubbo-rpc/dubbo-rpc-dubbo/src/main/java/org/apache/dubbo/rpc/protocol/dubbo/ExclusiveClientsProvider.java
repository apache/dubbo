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
import org.apache.dubbo.remoting.exchange.ExchangeClient;

import java.util.List;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_ERROR_CLOSE_CLIENT;

public class ExclusiveClientsProvider implements ClientsProvider {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(ExclusiveClientsProvider.class);
    private final List<ExchangeClient> clients;

    public ExclusiveClientsProvider(List<ExchangeClient> clients) {
        this.clients = clients;
    }

    @Override
    public List<ExchangeClient> getClients() {
        return clients;
    }

    @Override
    public void close(int timeout) {
        for (ExchangeClient client : clients) {
            try {
                client.close(timeout);
            } catch (Throwable t) {
                logger.warn(PROTOCOL_ERROR_CLOSE_CLIENT, "", "", t.getMessage(), t);
            }
        }
    }
}
