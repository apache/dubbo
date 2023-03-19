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
package org.apache.dubbo.rpc.protocol.rest;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
import org.apache.dubbo.metadata.rest.ServiceRestMetadata;
import org.apache.dubbo.remoting.http.RestClient;
import org.apache.dubbo.remoting.http.factory.RestClientFactory;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.ProtocolServer;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.AbstractExporter;
import org.apache.dubbo.rpc.protocol.AbstractProtocol;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.HttpConnectionPreBuildIntercept;
import org.apache.dubbo.rpc.protocol.rest.annotation.metadata.MetadataResolver;


import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_ERROR_CLOSE_CLIENT;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_ERROR_CLOSE_SERVER;
import static org.apache.dubbo.remoting.Constants.SERVER_KEY;
import static org.apache.dubbo.rpc.protocol.rest.constans.RestConstant.PATH_SEPARATOR;

public class RestProtocol extends AbstractProtocol {

    private static final int DEFAULT_PORT = 80;
    private static final String DEFAULT_SERVER = Constants.NETTY_HTTP;

    private final RestServerFactory serverFactory = new RestServerFactory();

    private final ConcurrentMap<String, ReferenceCountedClient<? extends RestClient>> clients = new ConcurrentHashMap<>();

    private final RestClientFactory clientFactory;

    private final Set<HttpConnectionPreBuildIntercept> httpConnectionPreBuildIntercepts;

    public RestProtocol(FrameworkModel frameworkModel) {
        this.clientFactory = frameworkModel.getExtensionLoader(RestClientFactory.class).getAdaptiveExtension();
        this.httpConnectionPreBuildIntercepts = frameworkModel.getExtensionLoader(HttpConnectionPreBuildIntercept.class).getSupportedExtensionInstances();
    }


    @Override
    public int getDefaultPort() {
        return DEFAULT_PORT;
    }


    @Override
    @SuppressWarnings("unchecked")
    public <T> Exporter<T> export(final Invoker<T> invoker) throws RpcException {
        URL url = invoker.getUrl();
        final String uri = serviceKey(url);
        Exporter<T> exporter = (Exporter<T>) exporterMap.get(uri);
        if (exporter != null) {
            // When modifying the configuration through override, you need to re-expose the newly modified service.
            if (Objects.equals(exporter.getInvoker().getUrl(), invoker.getUrl())) {
                return exporter;
            }
        }


        // resolve metadata
        ServiceRestMetadata serviceRestMetadata =
            MetadataResolver.resolveProviderServiceMetadata(url.getServiceModel().getProxyObject().getClass(),
                url, getContextPath(url));


        // TODO add Extension filter
        // create rest server
        RestProtocolServer server = (RestProtocolServer) ConcurrentHashMapUtils.computeIfAbsent(serverMap, getAddr(url), restServer -> {
            RestProtocolServer s = serverFactory.createServer(url.getParameter(SERVER_KEY, DEFAULT_SERVER));
            s.setAddress(url.getAddress());
            s.start(url);
            return s;
        });


        server.deploy(serviceRestMetadata, invoker);

        exporter = new AbstractExporter<T>(invoker) {
            @Override
            public void afterUnExport() {
                destroyInternal(url);
                exporterMap.remove(uri);
                server.undeploy(serviceRestMetadata);
            }
        };
        exporterMap.put(uri, exporter);
        return exporter;
    }


    @Override
    protected <T> Invoker<T> protocolBindingRefer(final Class<T> type, final URL url) throws RpcException {

        ReferenceCountedClient<? extends RestClient> refClient = clients.get(url.getAddress());
        if (refClient == null || refClient.isDestroyed()) {
            synchronized (clients) {
                refClient = clients.get(url.getAddress());
                if (refClient == null || refClient.isDestroyed()) {
                    refClient = ConcurrentHashMapUtils.computeIfAbsent(clients, url.getAddress(), _key -> createReferenceCountedClient(url));
                }
            }
        }
        refClient.retain();


        String contextPathFromUrl = getContextPath(url);

        // resolve metadata
        ServiceRestMetadata serviceRestMetadata =
            MetadataResolver.resolveConsumerServiceMetadata(type, url, contextPathFromUrl);

        Invoker<T> invoker = new RestInvoker<T>(type, url,
            refClient, httpConnectionPreBuildIntercepts, serviceRestMetadata);

        invokers.add(invoker);
        return invoker;
    }


    /**
     * create rest ReferenceCountedClient
     *
     * @param url
     * @return
     * @throws RpcException
     */
    private ReferenceCountedClient<? extends RestClient> createReferenceCountedClient(URL url) throws RpcException {

        // url -> RestClient
        RestClient restClient = clientFactory.createRestClient(url);

        return new ReferenceCountedClient<>(restClient, clients, clientFactory, url);
    }


    @Override
    public void destroy() {
        if (logger.isInfoEnabled()) {
            logger.info("Destroying protocol [" + this.getClass().getSimpleName() + "] ...");
        }
        super.destroy();

        for (Map.Entry<String, ProtocolServer> entry : serverMap.entrySet()) {
            try {
                if (logger.isInfoEnabled()) {
                    logger.info("Closing the rest server at " + entry.getKey());
                }
                entry.getValue().close();
            } catch (Throwable t) {
                logger.warn(PROTOCOL_ERROR_CLOSE_SERVER, "", "", "Error closing rest server", t);
            }
        }
        serverMap.clear();

        if (logger.isInfoEnabled()) {
            logger.info("Closing rest clients");
        }
        for (ReferenceCountedClient<?> client : clients.values()) {
            try {
                // destroy directly regardless of the current reference count.
                client.destroy();
            } catch (Throwable t) {
                logger.warn(PROTOCOL_ERROR_CLOSE_CLIENT, "", "", "Error closing rest client", t);
            }
        }
        clients.clear();
    }

    /**
     * getPath() will return: [contextpath + "/" +] path
     * 1. contextpath is empty if user does not set through ProtocolConfig or ProviderConfig
     * 2. path will never be empty, its default value is the interface name.
     *
     * @return return path only if user has explicitly gave then a value.
     */
    private String getContextPath(URL url) {
        String contextPath = url.getPath();
        if (contextPath != null) {
            if (contextPath.equalsIgnoreCase(url.getParameter(INTERFACE_KEY))) {
                return "";
            }
            if (contextPath.endsWith(url.getParameter(INTERFACE_KEY))) {
                contextPath = contextPath.substring(0, contextPath.lastIndexOf(url.getParameter(INTERFACE_KEY)));
            }
            return contextPath.endsWith(PATH_SEPARATOR) ? contextPath.substring(0, contextPath.length() - 1) : contextPath;
        } else {
            return "";
        }
    }


    private void destroyInternal(URL url) {
        try {
            ReferenceCountedClient<?> referenceCountedClient = clients.get(url.getAddress());
            if (referenceCountedClient != null && referenceCountedClient.release()) {
                clients.remove(url.getAddress());
            }
        } catch (Exception e) {
            logger.warn(PROTOCOL_ERROR_CLOSE_CLIENT, "", "", "Failed to close unused resources in rest protocol. interfaceName [" + url.getServiceInterface() + "]", e);
        }
    }
}
