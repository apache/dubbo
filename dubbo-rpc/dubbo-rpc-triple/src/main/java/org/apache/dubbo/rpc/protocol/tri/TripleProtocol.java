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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.exchange.PortUnificationExchanger;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.protocol.AbstractExporter;
import org.apache.dubbo.rpc.protocol.AbstractProtocol;
import org.apache.dubbo.rpc.support.ProtocolUtils;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class TripleProtocol extends AbstractProtocol implements Protocol {

    public static final String NAME = "triple";
    private static final Logger logger = LoggerFactory.getLogger(TripleProtocol.class);
    protected final Map<String, Exporter<?>> exporterMap = new ConcurrentHashMap<>();
    private final InvokerResolver serviceContainer = ExtensionLoader.getExtensionLoader(InvokerResolver.class).getDefaultExtension();

    protected static String serviceKey(URL url) {
        int port = url.getParameter(Constants.BIND_PORT_KEY, url.getPort());
        return serviceKey(port, url.getPath(), url.getVersion(), url.getGroup());
    }

    protected static String serviceKey(int port, String serviceName, String serviceVersion, String serviceGroup) {
        return ProtocolUtils.serviceKey(port, serviceName, serviceVersion, serviceGroup);
    }

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

        serviceContainer.add(url.getServiceKey(), invoker);
        if (!url.getServiceKey().equals(url.getServiceInterface())) {
            serviceContainer.add(url.getServiceInterface(), invoker);
        }
        PortUnificationExchanger.bind(invoker.getUrl());
        return exporter;
    }

    @Override
    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {

        // create rpc invoker.
        TripleInvoker<T> invoker = new TripleInvoker<T>(type, url, invokers);
        invokers.add(invoker);

        return invoker;
    }

    @Override
    protected <T> Invoker<T> protocolBindingRefer(Class<T> type, URL url) throws RpcException {
        return null;
    }

    @Override
    public void destroy() {
//        for (String path : path2Invoker.keySet()) {
//            final Invoker<?> invoker = path2Invoker.get(path);
//            if (invoker != null) {
//                path2Invoker.remove(path);
//                try {
//                    if (logger.isInfoEnabled()) {
//                        logger.info("Destroy reference for path=" + path + " url= " + invoker.getUrl());
//                    }
//                    invoker.destroy();
//                } catch (Throwable t) {
//                    logger.warn(t.getMessage(), t);
//                }
//            }
//        }

        for (String key : new ArrayList<>(exporterMap.keySet())) {
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
}
