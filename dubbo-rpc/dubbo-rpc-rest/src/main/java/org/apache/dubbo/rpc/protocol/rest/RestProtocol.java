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
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.http.HttpBinder;
import org.apache.dubbo.remoting.http.servlet.BootstrapListener;
import org.apache.dubbo.remoting.http.servlet.ServletManager;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.util.GetRestful;

import javax.servlet.ServletContext;

import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.remoting.Constants.SERVER_KEY;

public class RestProtocol extends AbstractHttpProtocol {

    private static final int DEFAULT_PORT = 80;
    private static final String DEFAULT_SERVER = "jetty";

    private final RestServerFactory serverFactory = new RestServerFactory();


    public void setHttpBinder(HttpBinder httpBinder) {
        serverFactory.setHttpBinder(httpBinder);
    }

    @Override
    public int getDefaultPort() {
        return DEFAULT_PORT;
    }

    @Override
    protected <T> Runnable doExport(T impl, Class<T> type, URL url) throws RpcException {
        String addr = getAddr(url);
        Class implClass = url.getServiceModel().getProxyObject().getClass();
        RestProtocolServer server = (RestProtocolServer) serverMap.computeIfAbsent(addr, restServer -> {
            RestProtocolServer s = serverFactory.createServer(url.getParameter(SERVER_KEY, DEFAULT_SERVER));
            s.setAddress(url.getAddress());
            s.start(url);
            return s;
        });

        String contextPath = getContextPath(url);
        if ("servlet".equalsIgnoreCase(url.getParameter(SERVER_KEY, DEFAULT_SERVER))) {
            ServletContext servletContext = ServletManager.getInstance().getServletContext(ServletManager.EXTERNAL_SERVER_PORT);
            if (servletContext == null) {
                throw new RpcException("No servlet context found. Since you are using server='servlet', " +
                    "make sure that you've configured " + BootstrapListener.class.getName() + " in web.xml");
            }
            String webappPath = servletContext.getContextPath();
            if (StringUtils.isNotEmpty(webappPath)) {
                webappPath = webappPath.substring(1);
                if (!contextPath.startsWith(webappPath)) {
                    throw new RpcException("Since you are using server='servlet', " +
                        "make sure that the 'contextpath' property starts with the path of external webapp");
                }
                contextPath = contextPath.substring(webappPath.length());
                if (contextPath.startsWith("/")) {
                    contextPath = contextPath.substring(1);
                }
            }
        }

        final Class resourceDef = GetRestful.getRootResourceClass(implClass) != null ? implClass : type;

        server.deploy(resourceDef, impl, contextPath);

        final RestProtocolServer s = server;
        return () -> {
            // TODO due to dubbo's current architecture,
            // it will be called from registry protocol in the shutdown process and won't appear in logs
            s.undeploy(resourceDef);
        };
    }

    @Override
    protected <T> Invoker<T> protocolBindingRefer(final Class<T> type, final URL url) throws RpcException {

        // create http invoker.
        HttpInvoker<T> invoker = new HttpInvoker<>(type, url, getTarget(type,url));
        invokers.add(invoker);

        return invoker;
    }

    private <T> Invoker<T> getTarget(final Class<T> type, final URL url) {

        ReferenceCountedClient referenceCountedClient = getOrCreatePoolClient(url);

        // TODO httpclient proxy ?
        ResteasyWebTarget target = referenceCountedClient.getClient().target("http://" + url.getAddress() + "/" + getContextPath(url));
        T proxy = target.proxy(type);

        return proxyFactory.getInvoker(proxy,type,url);
    }


    @Override
    protected int getErrorCode(Throwable e) {
        // TODO
        return super.getErrorCode(e);
    }

    /**
     * getPath() will return: [contextpath + "/" +] path
     * 1. contextpath is empty if user does not set through ProtocolConfig or ProviderConfig
     * 2. path will never be empty, its default value is the interface name.
     *
     * @return return path only if user has explicitly gave then a value.
     */
    protected String getContextPath(URL url) {
        String contextPath = url.getPath();
        if (contextPath != null) {
            if (contextPath.equalsIgnoreCase(url.getParameter(INTERFACE_KEY))) {
                return "";
            }
            if (contextPath.endsWith(url.getParameter(INTERFACE_KEY))) {
                contextPath = contextPath.substring(0, contextPath.lastIndexOf(url.getParameter(INTERFACE_KEY)));
            }
            return contextPath.endsWith("/") ? contextPath.substring(0, contextPath.length() - 1) : contextPath;
        } else {
            return "";
        }
    }


    @Override
    protected <T> T doRefer(Class<T> type, URL url) throws RpcException {
        throw new UnsupportedOperationException("not used");
    }
}
