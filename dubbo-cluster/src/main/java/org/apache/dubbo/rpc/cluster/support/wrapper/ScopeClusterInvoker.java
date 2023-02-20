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
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.ClusterInvoker;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.protocol.injvm.InjvmExporterListener;
import org.apache.dubbo.rpc.protocol.injvm.InjvmProtocol;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.dubbo.common.constants.CommonConstants.LOCALHOST_VALUE;
import static org.apache.dubbo.rpc.Constants.LOCAL_PROTOCOL;
import static org.apache.dubbo.rpc.Constants.SCOPE_KEY;
import static org.apache.dubbo.rpc.Constants.SCOPE_REMOTE;
import static org.apache.dubbo.rpc.Constants.SCOPE_LOCAL;

/**
 * A ClusterInvoker that selects between local and remote invokers at runtime.
 */
public class ScopeClusterInvoker<T> implements ClusterInvoker<T>, InjvmExporterListener {

    private Protocol protocolSPI;
    private final Directory<T> directory;
    private final Invoker<T> invoker;
    private final AtomicBoolean isExported = new AtomicBoolean(false);
    private volatile Invoker<T> injvmInvoker;
    private boolean localFlag;
    private InjvmProtocol injvmProtocol;


    public ScopeClusterInvoker(Directory<T> directory, Invoker<T> invoker) {
        this.directory = directory;
        this.invoker = invoker;
        this.localFlag = SCOPE_LOCAL.equalsIgnoreCase(directory.getUrl().getParameter(SCOPE_KEY));
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
            if (injvmProtocol == null) {
                createInjvmProtocol();
            }
            if (protocolSPI == null) {
                protocolSPI = ApplicationModel.defaultModel().getExtensionLoader(Protocol.class).getAdaptiveExtension();
            }
            if (isExported.get() && !localFlag) {
                localFlag = injvmProtocol.isInjvmRefer(getUrl());
            }
            if (!isExported.get() && SCOPE_LOCAL.equalsIgnoreCase(scope)) {
                throw new RpcException("InjvmInvoker has not been exposed yet!");
            }
            if (localFlag) {
                return selectInjvmInvoker().invoke(invocation);
            }
        }
        return invoker.invoke(invocation);
    }

    private void createInjvmProtocol() {
        injvmProtocol = InjvmProtocol.getInjvmProtocol(getUrl().getScopeModel());
        if (injvmProtocol.attach(injvmProtocol.invokerCacheKey(getUrl()), this, getUrl())) {
            notifyExporter();
        }
    }


    private Invoker<T> selectInjvmInvoker() {
        if (injvmInvoker != null) {
            return injvmInvoker;
        }
        if (!localFlag) {
            throw new RpcException("This call cannot be called as a remote call.");
        }
        if (injvmInvoker == null) {
            synchronized (this) {
                if (injvmInvoker == null) {
                    injvmInvoker = createInjvmInvoker();
                }
            }
        }
        if (injvmInvoker != null) {
            return injvmInvoker;
        } else {
            throw new RpcException("Failed to create local call Invoker.");
        }
    }

    private Invoker<T> createInjvmInvoker() {
        URL url = new ServiceConfigURL(LOCAL_PROTOCOL, LOCALHOST_VALUE, 0, getDirectory().getInterface().getName(), getUrl().getParameters());
        url = url.setScopeModel(getUrl().getScopeModel());
        url = url.setServiceModel(getUrl().getServiceModel());
        return protocolSPI.refer(getDirectory().getInterface(), url);
    }


    @Override
    public void notifyExporter() {
        isExported.compareAndSet(false, true);
    }
}
