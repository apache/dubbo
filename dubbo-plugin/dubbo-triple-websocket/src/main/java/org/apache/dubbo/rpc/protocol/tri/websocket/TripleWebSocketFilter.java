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
package org.apache.dubbo.rpc.protocol.tri.websocket;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.remoting.http12.HttpMethods;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_FAILED_REQUEST;
import static org.apache.dubbo.rpc.protocol.tri.TripleConstants.UPGRADE_HEADER_KEY;
import static org.apache.dubbo.rpc.protocol.tri.websocket.WebSocketConstants.TRIPLE_WEBSOCKET_REMOTE_ADDRESS;
import static org.apache.dubbo.rpc.protocol.tri.websocket.WebSocketConstants.TRIPLE_WEBSOCKET_UPGRADE_HEADER_VALUE;

public class TripleWebSocketFilter implements Filter {

    private static final ErrorTypeAwareLogger LOG = LoggerFactory.getErrorTypeAwareLogger(TripleWebSocketFilter.class);

    private transient ServerContainer sc;

    private final Set<String> existed = new ConcurrentHashSet<>();

    @Override
    public void init(FilterConfig filterConfig) {
        sc = (ServerContainer) filterConfig.getServletContext().getAttribute(ServerContainer.class.getName());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!isWebSocketUpgradeRequest(request, response)) {
            chain.doFilter(request, response);
            return;
        }
        HttpServletRequest hRequest = (HttpServletRequest) request;
        HttpServletResponse hResponse = (HttpServletResponse) response;
        String path;
        String pathInfo = hRequest.getPathInfo();
        if (pathInfo == null) {
            path = hRequest.getServletPath();
        } else {
            path = hRequest.getServletPath() + pathInfo;
        }
        Map<String, String[]> copiedMap = new HashMap<>(hRequest.getParameterMap());
        copiedMap.put(
                TRIPLE_WEBSOCKET_REMOTE_ADDRESS,
                new String[] {hRequest.getRemoteHost(), String.valueOf(hRequest.getRemotePort())});
        HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper(hRequest) {
            @Override
            public Map<String, String[]> getParameterMap() {
                return copiedMap;
            }
        };
        if (existed.contains(path)) {
            chain.doFilter(wrappedRequest, hResponse);
            return;
        }
        ServerEndpointConfig serverEndpointConfig =
                ServerEndpointConfig.Builder.create(TripleEndpoint.class, path).build();
        try {
            sc.addEndpoint(serverEndpointConfig);
            existed.add(path);
        } catch (Exception e) {
            LOG.error(PROTOCOL_FAILED_REQUEST, "", "", "Failed to add endpoint", e);
            hResponse.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        chain.doFilter(wrappedRequest, hResponse);
    }

    @Override
    public void destroy() {}

    public boolean isWebSocketUpgradeRequest(ServletRequest request, ServletResponse response) {
        return ((request instanceof HttpServletRequest)
                && (response instanceof HttpServletResponse)
                && headerContainsToken(
                        (HttpServletRequest) request, UPGRADE_HEADER_KEY, TRIPLE_WEBSOCKET_UPGRADE_HEADER_VALUE)
                && HttpMethods.GET.name().equals(((HttpServletRequest) request).getMethod()));
    }

    private boolean headerContainsToken(HttpServletRequest req, String headerName, String target) {
        Enumeration<String> headers = req.getHeaders(headerName);
        while (headers.hasMoreElements()) {
            String header = headers.nextElement();
            String[] tokens = header.split(",");
            for (String token : tokens) {
                if (target.equalsIgnoreCase(token.trim())) {
                    return true;
                }
            }
        }
        return false;
    }
}
