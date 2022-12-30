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

import org.apache.dubbo.common.ServiceKey;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.executor.AbstractIsolationExecutorSupport;

import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;

public class DubboIsolationExecutorSupport extends AbstractIsolationExecutorSupport {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(DubboIsolationExecutorSupport.class);

    public DubboIsolationExecutorSupport(URL url) {
        super(url);
    }

    @Override
    protected ServiceKey getServiceKey(Object data) {
        if (!(data instanceof Request)) {
            return null;
        }

        Request request = (Request) data;
        if (!(request.getData() instanceof Invocation)) {
            return null;
        }
        Invocation inv = (Invocation) request.getData();
        Map<String, String> attachments = inv.getAttachments();
        String interfaceName = attachments.get(PATH_KEY);
        String version = attachments.get(VERSION_KEY);
        String group = attachments.get(GROUP_KEY);
        return new ServiceKey(interfaceName, version, group);
    }
}
