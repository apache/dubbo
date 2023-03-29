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
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Cluster;
import org.apache.dubbo.rpc.cluster.ClusterInvoker;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.directory.StaticDirectory;
import org.apache.dubbo.rpc.listener.ExporterChangeListener;
import org.apache.dubbo.rpc.listener.InjvmExporterListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.dubbo.rpc.Constants.GENERIC_KEY;
import static org.apache.dubbo.rpc.Constants.LOCAL_KEY;
import static org.apache.dubbo.rpc.Constants.LOCAL_PROTOCOL;
import static org.apache.dubbo.rpc.Constants.SCOPE_KEY;
import static org.apache.dubbo.rpc.Constants.SCOPE_REMOTE;
import static org.apache.dubbo.rpc.Constants.SCOPE_LOCAL;
import static org.apache.dubbo.rpc.cluster.Constants.PEER_KEY;

/**
 * A ClusterInvoker that selects between local and remote invokers at runtime.
 */
public class ScopeClusterInvoker<T> implements ClusterInvoker<T>, ExporterChangeListener {

    private Protocol protocolSPI;
    private final Directory<T> directory;
    private final Invoker<T> invoker;
    private final AtomicBoolean isExported;
    private volatile Invoker<T> injvmInvoker;
    private volatile InjvmExporterListener injvmExporterListener;

    private boolean peerFlag;

    private boolean injvmFlag;

    private final Object createLock = new Object();


    public ScopeClusterInvoker(Directory<T> directory, Invoker<T> invoker) {
        this.directory = directory;
        this.invoker = invoker;
        this.isExported = new AtomicBoolean(false);
        init();
    }

    private void init() {
        Boolean peer = (Boolean) getUrl().getAttribute(PEER_KEY);
        String isInjvm = getUrl().getParameter(LOCAL_PROTOCOL);
        if (peer != null && peer) {
            peerFlag = true;
            return;
        }
        if (injvmInvoker == null && LOCAL_PROTOCOL.equalsIgnoreCase(getRegistryUrl().getProtocol())) {
            injvmInvoker = invoker;
            isExported.compareAndSet(false, true);
            injvmFlag = true;
            return;
        }
        if (Boolean.TRUE.toString().equalsIgnoreCase(isInjvm) || LOCAL_KEY.equalsIgnoreCase(getUrl().getParameter(SCOPE_KEY))) {
            injvmFlag = true;
        } else if (isInjvm == null) {
            injvmFlag = isNotRemoteOrGeneric();
        }

        protocolSPI = getUrl().getApplicationModel().getExtensionLoader(Protocol.class).getAdaptiveExtension();
        injvmExporterListener = getUrl().getOrDefaultFrameworkModel().getBeanFactory().getBean(InjvmExporterListener.class);
        injvmExporterListener.addExporterChangeListener(this, getUrl().getServiceKey());
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
        return isExported.get() || directory.isAvailable();
    }

    @Override
    public void destroy() {
        if (injvmExporterListener != null) {
            injvmExporterListener.removeExporterChangeListener(this, getUrl().getServiceKey());
        }
        destroyInjvmInvoker();
        this.invoker.destroy();
    }

    @Override
    public Class<T> getInterface() {
        return directory.getInterface();
    }

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        if (peerFlag) {
            return invoker.invoke(invocation);
        }
        if (isInjvmExported()) {
            return injvmInvoker.invoke(invocation);
        }
        return invoker.invoke(invocation);
    }

    private boolean isNotRemoteOrGeneric() {
        return !SCOPE_REMOTE.equalsIgnoreCase(getUrl().getParameter(SCOPE_KEY)) &&
            !getUrl().getParameter(GENERIC_KEY, false);
    }

    private boolean isInjvmExported() {
        Boolean localInvoke = RpcContext.getServiceContext().getLocalInvoke();
        boolean isExportedValue = isExported.get();
        boolean local = (localInvoke != null && localInvoke);
        // Determine whether this call is local
        if (isExportedValue && local) {
            return true;
        }

        // Determine whether this call is remote
        if (localInvoke != null && !localInvoke) {
            return false;
        }

        // When calling locally, determine whether it does not meet the requirements
        if (!isExportedValue && (SCOPE_LOCAL.equalsIgnoreCase(getUrl().getParameter(SCOPE_KEY)) ||
            Boolean.TRUE.toString().equalsIgnoreCase(getUrl().getParameter(LOCAL_PROTOCOL))|| local)) {
            throw new RpcException("Local service has not been exposed yet!");
        }

        return isExportedValue && injvmFlag;
    }


    private void createInjvmInvoker() {
        if (injvmInvoker == null) {
            synchronized (createLock) {
                if (injvmInvoker == null) {
                    URL url = new ServiceConfigURL(LOCAL_PROTOCOL, NetUtils.getLocalHost(), getUrl().getPort(), getInterface().getName(), getUrl().getParameters());
                    url = url.setScopeModel(getUrl().getScopeModel());
                    url = url.setServiceModel(getUrl().getServiceModel());
                    Invoker<?> invoker = protocolSPI.refer(getInterface(), url);
                    List<Invoker<?>> invokers = new ArrayList<>();
                    invokers.add(invoker);
                    injvmInvoker = Cluster.getCluster(url.getScopeModel(), Cluster.DEFAULT, false).join(new StaticDirectory(url, invokers), true);
                }
            }
        }
    }

    @Override
    public void onExporterChangeExport(Exporter<?> exporter) {
        if (isExported.get()) {
            return;
        }
        if (getUrl().getServiceKey().equals(exporter.getInvoker().getUrl().getServiceKey())
            && exporter.getInvoker().getUrl().getProtocol().equalsIgnoreCase(LOCAL_PROTOCOL)) {
            createInjvmInvoker();
            isExported.compareAndSet(false, true);
        }
    }

    @Override
    public void onExporterChangeUnExport(Exporter<?> exporter) {
        if (getUrl().getServiceKey().equals(exporter.getInvoker().getUrl().getServiceKey())
            && exporter.getInvoker().getUrl().getProtocol().equalsIgnoreCase(LOCAL_PROTOCOL)) {
            destroyInjvmInvoker();
            isExported.compareAndSet(true, false);
        }
    }

    private void destroyInjvmInvoker() {
        if (injvmInvoker != null) {
            injvmInvoker.destroy();
            injvmInvoker = null;
        }
    }

    public Invoker<?> getInvoker() {
        return invoker;
    }
}
