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
package org.apache.dubbo.rpc.protocol.tri;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.RemotingServer;
import org.apache.dubbo.remoting.exchange.PortUnificationExchanger;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProtocolServer;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.protocol.AbstractExporter;
import org.apache.dubbo.rpc.support.ProtocolUtils;

/**
 *
 */
public class TripleProtocol implements Protocol {

    private static ConcurrentHashMap<String, Invoker> path2Invoker = new ConcurrentHashMap<>();
    private static final String NAME = "triple";
    private static TripleProtocol INSTANCE;
    private static final Logger logger = LoggerFactory.getLogger(TripleProtocol.class);
    protected final Map<String, Exporter<?>> exporterMap = new ConcurrentHashMap<String, Exporter<?>>();

    @Override
    public int getDefaultPort() {
        return 50051;
    }

    @Override
    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        URL url = invoker.getUrl();
        String key = serviceKey(url);
        final AbstractExporter<T> exporter = new AbstractExporter<T>(invoker) {
            @Override
            public void unexport() {
                super.unexport();
                exporterMap.remove(key);
            }
        };

        exporterMap.put(key, exporter);
        path2Invoker.put(invoker.getUrl().getServiceKey(), invoker);
        PortUnificationExchanger.bind(invoker.getUrl());
        return exporter;
    }

    protected static String serviceKey(URL url) {
        int port = url.getParameter(Constants.BIND_PORT_KEY, url.getPort());
        return serviceKey(port, url.getPath(), url.getVersion(), url.getGroup());
    }

    protected static String serviceKey(int port, String serviceName, String serviceVersion, String serviceGroup) {
        return ProtocolUtils.serviceKey(port, serviceName, serviceVersion, serviceGroup);
    }

    @Override
    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
        // TODO fill me
        return null;
    }

    @Override
    public void destroy() {

        for (Invoker<?> invoker : path2Invoker.values()) {
            if (invoker != null) {
                path2Invoker.remove(invoker);
                try {
                    if (logger.isInfoEnabled()) {
                        logger.info("Destroy reference: " + invoker.getUrl());
                    }
                    invoker.destroy();
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            }
        }

        for (String key : new ArrayList<String>(exporterMap.keySet())) {
            Exporter<?> exporter = exporterMap.remove(key);
            if (exporter != null) {
                try {
                    if (logger.isInfoEnabled()) {
                        logger.info("Unexport service: " + exporter.getInvoker().getUrl());
                    }
                    exporter.unexport();
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            }
        }

        PortUnificationExchanger.close();

    }

    public Invoker getInvoker(String path) {
        return path2Invoker.get(path);
    }

    public TripleProtocol() {
        INSTANCE = this;
    }

    public static TripleProtocol getTripleProtocol() {
        if (INSTANCE == null) {
            // load
            ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(TripleProtocol.NAME);
        }

        return INSTANCE;
    }
}
