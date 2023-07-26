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
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.executor.AbstractIsolationExecutorSupport;
import org.apache.dubbo.rpc.model.FrameworkServiceRepository;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceModel;

public class DubboIsolationExecutorSupport extends AbstractIsolationExecutorSupport {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(DubboIsolationExecutorSupport.class);

    private final FrameworkServiceRepository frameworkServiceRepository;
    private final DubboProtocol dubboProtocol;

    public DubboIsolationExecutorSupport(URL url) {
        super(url);
        frameworkServiceRepository = url.getOrDefaultFrameworkModel().getServiceRepository();
        dubboProtocol = DubboProtocol.getDubboProtocol(url.getOrDefaultFrameworkModel());
    }

    @Override
    protected ProviderModel getProviderModel(Object data) {
        if (!(data instanceof Request)) {
            return null;
        }

        Request request = (Request) data;
        if (!(request.getData() instanceof DecodeableRpcInvocation)) {
            return null;
        }

        try {
            ((DecodeableRpcInvocation) request.getData()).fillInvoker(dubboProtocol);
        } catch (RemotingException e) {
            // ignore here, and this exception will being rethrow in DubboProtocol
        }

        ServiceModel serviceModel = ((Invocation) request.getData()).getServiceModel();
        if (serviceModel instanceof ProviderModel) {
            return (ProviderModel) serviceModel;
        }

        String targetServiceUniqueName = ((Invocation) request.getData()).getTargetServiceUniqueName();
        if (StringUtils.isNotEmpty(targetServiceUniqueName)) {
            return frameworkServiceRepository.lookupExportedService(targetServiceUniqueName);
        }

        return null;
    }
}
