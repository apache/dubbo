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
import org.apache.dubbo.rpc.ExporterListener;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.ClusterInvoker;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.listener.ExporterChangeListener;
import org.apache.dubbo.rpc.listener.InjvmExporterListener;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.dubbo.rpc.Constants.GENERIC_KEY;
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


    public ScopeClusterInvoker(Directory<T> directory, Invoker<T> invoker) {
        this.directory = directory;
        this.invoker = invoker;
        this.isExported = new AtomicBoolean(false);
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
        if (injvmExporterListener == null) {
            injvmExporterListener = (InjvmExporterListener) getUrl().getApplicationModel().getExtensionLoader(ExporterListener.class).getExtension(LOCAL_PROTOCOL);
        }
        injvmExporterListener.addExporterChangeListener(this, getUrl().getServiceKey());
        return isExported.get() || directory.isAvailable();
    }

    @Override
    public void destroy() {
        if (injvmExporterListener != null) {
            injvmExporterListener.removeExporterChangeListener(this);
        }
        this.invoker.destroy();
    }

    @Override
    public Class<T> getInterface() {
        return directory.getInterface();
    }

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        Boolean peer = (Boolean) getUrl().getAttribute(PEER_KEY);
        if (peer != null && peer) {
            return invoker.invoke(invocation);
        }
        String scope = getUrl().getParameter(SCOPE_KEY);

        if (shouldInvokeInjvm(getUrl().getParameter(LOCAL_PROTOCOL), scope)) {
            return selectInjvmInvoker().invoke(invocation);
        }
        return invoker.invoke(invocation);
    }

    private boolean shouldInvokeInjvm(String isInjvm, String scope) {
        if (injvmInvoker == null && LOCAL_PROTOCOL.equals(getRegistryUrl().getProtocol())) {
            isExported.compareAndSet(false, true);
            injvmInvoker = invoker;
            return true;
        }
        if (Boolean.TRUE.toString().equals(isInjvm)) {
            return isInjvmExported(scope);
        } else if (isInjvm == null) {
            return isInjvmExportedAndNotRemoteOrGeneric(scope);
        }
        return false;
    }

    private boolean isInjvmExportedAndNotRemoteOrGeneric(String scope) {
        return !SCOPE_REMOTE.equalsIgnoreCase(scope) &&
            !getUrl().getParameter(GENERIC_KEY, false) && isInjvmExported(scope);
    }

    private boolean isInjvmExported(String scope) {
        if (injvmExporterListener == null) {
            injvmExporterListener = (InjvmExporterListener) getUrl().getApplicationModel().getExtensionLoader(ExporterListener.class).getExtension("injvm");
            injvmExporterListener.addExporterChangeListener(this, getUrl().getServiceKey());
        }
        if (!isExported.get() && SCOPE_LOCAL.equalsIgnoreCase(scope)) {
            throw new RpcException("Local service has not been exposed yet!");
        }
        if (isExported.get()) {
            return true;
        }
        return false;
    }


    private Invoker<T> selectInjvmInvoker() {
        if (injvmInvoker != null) {
            return injvmInvoker;
        }
        if (injvmInvoker == null && protocolSPI == null) {
            protocolSPI = getUrl().getApplicationModel().getExtensionLoader(Protocol.class).getAdaptiveExtension();
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
        URL url = new ServiceConfigURL(LOCAL_PROTOCOL, NetUtils.getLocalHost(), getUrl().getPort(), getDirectory().getInterface().getName(), getUrl().getParameters());
        url = url.setScopeModel(getUrl().getScopeModel());
        url = url.setServiceModel(getUrl().getServiceModel());
        return protocolSPI.refer(getDirectory().getInterface(), url);
    }

    @Override
    public void onExporterChangeExport(Map<String, Exporter<?>> exporters) {
        if (isExported.get()) {
            return;
        }
        Exporter<?> exporter = exporters.get(getUrl().getServiceKey());
        if (getUrl().getServiceKey().equals(exporter.getInvoker().getUrl().getServiceKey())
            && exporter.getInvoker().getUrl().getProtocol().equals(LOCAL_PROTOCOL)) {
            isExported.compareAndSet(false, true);
        }
    }

    @Override
    public void onExporterChangeUnExport(Exporter<?> exporter) {
        if (getUrl().getServiceKey().equals(exporter.getInvoker().getUrl().getServiceKey())
            && exporter.getInvoker().getUrl().getProtocol().equals(LOCAL_PROTOCOL)) {
            isExported.compareAndSet(true, false);
        }
    }

    public Invoker<?> getInvoker() {
        return invoker;
    }
}
