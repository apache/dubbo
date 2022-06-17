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

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.reference.ReferenceCountedResource;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;

public class ReferenceCountedClient extends ReferenceCountedResource {
    private static final Logger logger = LoggerFactory.getLogger(ReferenceCountedClient.class);

    private final ResteasyClient resteasyClient;

    public ReferenceCountedClient(ResteasyClient resteasyClient) {
        this.resteasyClient = resteasyClient;
    }

    public ResteasyClient getClient() {
        return resteasyClient;
    }

    public boolean isDestroyed() {
        return resteasyClient.isClosed();
    }

    @Override
    protected void destroy() {
        try {
            resteasyClient.close();
        } catch (Exception e) {
            logger.error("Close resteasy client error", e);
        }
    }
}
