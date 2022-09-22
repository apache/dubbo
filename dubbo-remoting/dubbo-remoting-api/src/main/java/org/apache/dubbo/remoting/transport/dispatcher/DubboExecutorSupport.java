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
package org.apache.dubbo.remoting.transport.dispatcher;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.rpc.support.ExecutorSupport;
import org.apache.dubbo.rpc.support.TripleTuple;

import java.lang.reflect.Method;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;

public class DubboExecutorSupport extends ExecutorSupport {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(DubboExecutorSupport.class);
    private static final String INVOCATION_GET_ATTACHMENTS_METHOD = "getAttachments";

    public DubboExecutorSupport(URL url) {
        super(url);
    }

    @Override
    protected TripleTuple getTripleTuple(Object data) {
        if (!(data instanceof Request)) {
            return null;
        }

        try {
            Request request = (Request) data;
            Method method = request.getData().getClass().getMethod(INVOCATION_GET_ATTACHMENTS_METHOD);
            Map<String, String> attachments = (Map<String, String>) method.invoke(request.getData());
            String serviceName = attachments.get(PATH_KEY);
            String version = attachments.get(VERSION_KEY);
            String group = attachments.get(GROUP_KEY);
            return new TripleTuple(serviceName, version, group);
        } catch (Throwable e) {
            logger.error("failed to get TripleTuple, maybe the build rule for data is wrong, data = " + data, e);
        }

        return null;
    }
}
