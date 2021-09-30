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
package org.apache.dubbo.demo.provider.chain;

import org.apache.dubbo.demo.ChainService;
import org.apache.dubbo.rpc.RpcContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChainServiceImpl implements ChainService {
    private static final Logger logger = LoggerFactory.getLogger(ChainServiceImpl.class);

    @Override
    public String chain(String input) {
        logger.info("Received " + input + ", request from consumer: " + RpcContext.getServiceContext().getRemoteAddress());
        return "Received " + input + ", response from provider: " + RpcContext.getServiceContext().getLocalAddress();
    }
}
