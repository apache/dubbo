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

package org.apache.dubbo.rpc.cluster.support.wrapper;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.ClusterInvoker;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.protocol.injvm.InjvmProtocol;

import static org.apache.dubbo.common.constants.CommonConstants.LOCALHOST_VALUE;
import static org.apache.dubbo.rpc.Constants.LOCAL_PROTOCOL;
import static org.apache.dubbo.rpc.Constants.SCOPE_KEY;
import static org.apache.dubbo.rpc.Constants.SCOPE_REMOTE;
import static org.apache.dubbo.rpc.Constants.SCOPE_LOCAL;

/**
 * Judge whether to use local calls at runtime to avoid the problem that local calls cannot be used due to delayed
 * exposure of local services. In the future, rules will be added to make the switch between local calls and remote calls more flexible.
 */
public class ScopeClusterInvoker<T> implements ClusterInvoker<T> {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(MockClusterInvoker.class);

    private final Directory<T> directory;

    private final Invoker<T> invoker;

    private Invoker<T> injvmInvoker;

    private Protocol protocolSPI;

    private boolean localFlag;


    public ScopeClusterInvoker(Directory<T> directory, Invoker<T> invoker) {
        this.directory = directory;
        this.invoker = invoker;
    }

    @Override
    public URL getUrl() {
        return directory.getConsumerUrl();
    }

    @Override
    public URL getRegistryUrl() {
        return directory.getUrl();
    }

    @Override
    public Directory<T> getDirectory() {
        return directory;
    }

    @Override
    public boolean isDestroyed() {
        return directory.isDestroyed();
    }

    @Override
    public boolean isAvailable() {
        return directory.isAvailable();
    }

    @Override
    public void destroy() {
        this.invoker.destroy();
    }

    @Override
    public Class<T> getInterface() {
        return directory.getInterface();
    }

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        String scope = getUrl().getParameter(SCOPE_KEY);
        if (!SCOPE_REMOTE.equalsIgnoreCase(scope)) {
            // Avoid duplicate creation
            if ("injvm".equalsIgnoreCase(getUrl().getProtocol())) {
                localFlag = true;
            }
            if (!localFlag) {
                // TODO If the local service has not been exposed, it will waste
                //  performance to carry out unnecessary verification all the time. Need improvement
                localFlag = InjvmProtocol.getInjvmProtocol(getUrl().getScopeModel()).isInjvmRefer(getUrl());
            }
            if (localFlag || SCOPE_LOCAL.equalsIgnoreCase(scope)) {
                return selectInjvmInvoker().invoke(invocation);
            }
        }
        return this.invoker.invoke(invocation);
    }


    private Invoker<T> selectInjvmInvoker() {
        if (injvmInvoker != null) {
            return injvmInvoker;
        }
        if (!localFlag) {
            throw new RpcException("This call cannot be called as a remote call.");
        }
        URL url = new ServiceConfigURL(LOCAL_PROTOCOL, LOCALHOST_VALUE, 0, getDirectory().getInterface().getName(), getUrl().getParameters());
        url = url.setScopeModel(getUrl().getScopeModel());
        url = url.setServiceModel(getUrl().getServiceModel());
        if (protocolSPI == null) {
            protocolSPI = ApplicationModel.defaultModel().getExtensionLoader(Protocol.class).getAdaptiveExtension();
        }
        Invoker<T> invoker = protocolSPI.refer(getDirectory().getInterface(), url);
        if (invoker != null) {
            injvmInvoker = invoker;
            return injvmInvoker;
        } else {
            throw new RpcException("Failed to create local call Invoker.");
        }
    }

}
