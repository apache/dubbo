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
package org.apache.dubbo.rpc.protocol;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.serialize.support.SerializableClassRegistry;
import org.apache.dubbo.common.serialize.support.SerializationOptimizer;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProtocolServer;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ScopeModelAware;
import org.apache.dubbo.rpc.support.ProtocolUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.dubbo.common.constants.CommonConstants.ANYHOST_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.ANYHOST_VALUE;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_SERVER_SHUTDOWN_TIMEOUT;
import static org.apache.dubbo.common.constants.CommonConstants.OPTIMIZER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SHUTDOWN_WAIT_KEY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_FAILED_DESTROY_INVOKER;

/**
 * abstract ProtocolSupport.
 */
public abstract class AbstractProtocol implements Protocol, ScopeModelAware {

    protected final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());

    protected final Map<String, Exporter<?>> exporterMap = new ConcurrentHashMap<>();

    /**
     * <host:port, ProtocolServer>
     */
    protected final ConcurrentMap<String, ProtocolServer> serverMap = new ConcurrentHashMap<>();

    // TODO SoftReference
    protected final Set<Invoker<?>> invokers = new ConcurrentHashSet<>();

    protected FrameworkModel frameworkModel;

    private final Set<String> optimizers = new ConcurrentHashSet<>();


    @Override
    public void setFrameworkModel(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    protected static String serviceKey(URL url) {
        int port = url.getParameter(Constants.BIND_PORT_KEY, url.getPort());
        return serviceKey(port, url.getPath(), url.getVersion(), url.getGroup());
    }

    protected static String serviceKey(int port, String serviceName, String serviceVersion, String serviceGroup) {
        return ProtocolUtils.serviceKey(port, serviceName, serviceVersion, serviceGroup);
    }

    @Override
    public List<ProtocolServer> getServers() {
        return Collections.unmodifiableList(new ArrayList<>(serverMap.values()));
    }

    protected void loadServerProperties(ProtocolServer server) {
        // read and hold config before destroy
        int serverShutdownTimeout = ConfigurationUtils.getServerShutdownTimeout(server.getUrl().getScopeModel());
        server.getAttributes().put(SHUTDOWN_WAIT_KEY, serverShutdownTimeout);
    }

    protected int getServerShutdownTimeout(ProtocolServer server) {
        return (int) server.getAttributes().getOrDefault(SHUTDOWN_WAIT_KEY, DEFAULT_SERVER_SHUTDOWN_TIMEOUT);
    }

    @Override
    public void destroy() {
        for (Invoker<?> invoker : invokers) {
            if (invoker != null) {
                try {
                    if (logger.isInfoEnabled()) {
                        logger.info("Destroy reference: " + invoker.getUrl());
                    }
                    invoker.destroy();
                } catch (Throwable t) {
                    logger.warn(PROTOCOL_FAILED_DESTROY_INVOKER, "", "", t.getMessage(), t);
                }
            }
        }
        invokers.clear();

        exporterMap.forEach((key, exporter) -> {
            if (exporter != null) {
                try {
                    if (logger.isInfoEnabled()) {
                        logger.info("Unexport service: " + exporter.getInvoker().getUrl());
                    }
                    exporter.unexport();
                } catch (Throwable t) {
                    logger.warn(PROTOCOL_FAILED_DESTROY_INVOKER, "", "", t.getMessage(), t);
                }
            }
        });
        exporterMap.clear();
    }

    @Override
    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
        return protocolBindingRefer(type, url);
    }

    @Deprecated
    protected abstract <T> Invoker<T> protocolBindingRefer(Class<T> type, URL url) throws RpcException;

    public Map<String, Exporter<?>> getExporterMap() {
        return exporterMap;
    }

    public Collection<Exporter<?>> getExporters() {
        return Collections.unmodifiableCollection(exporterMap.values());
    }


    protected void optimizeSerialization(URL url) throws RpcException {
        String className = url.getParameter(OPTIMIZER_KEY, "");
        if (StringUtils.isEmpty(className) || optimizers.contains(className)) {
            return;
        }

        logger.info("Optimizing the serialization process for Kryo, FST, etc...");

        try {
            Class clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
            if (!SerializationOptimizer.class.isAssignableFrom(clazz)) {
                throw new RpcException("The serialization optimizer " + className + " isn't an instance of " + SerializationOptimizer.class.getName());
            }

            SerializationOptimizer optimizer = (SerializationOptimizer) clazz.newInstance();

            if (optimizer.getSerializableClasses() == null) {
                return;
            }

            for (Class c : optimizer.getSerializableClasses()) {
                SerializableClassRegistry.registerClass(c);
            }

            optimizers.add(className);

        } catch (ClassNotFoundException e) {
            throw new RpcException("Cannot find the serialization optimizer class: " + className, e);

        } catch (InstantiationException | IllegalAccessException e) {
            throw new RpcException("Cannot instantiate the serialization optimizer class: " + className, e);

        }
    }

    protected String getAddr(URL url) {
        String bindIp = url.getParameter(org.apache.dubbo.remoting.Constants.BIND_IP_KEY, url.getHost());
        if (url.getParameter(ANYHOST_KEY, false)) {
            bindIp = ANYHOST_VALUE;
        }
        return NetUtils.getIpByHost(bindIp) + ":" + url.getParameter(org.apache.dubbo.remoting.Constants.BIND_PORT_KEY, url.getPort());
    }

}
