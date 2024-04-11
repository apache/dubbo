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
package org.apache.dubbo.rpc.protocol.tri.rest.cors;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.remoting.http12.HttpStatus;
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.apache.dubbo.rpc.protocol.tri.rest.RestConstants.ACCESS_CONTROL_ALLOW_PRIVATE_NETWORK;
import static org.apache.dubbo.rpc.protocol.tri.rest.RestConstants.ACCESS_CONTROL_REQUEST_PRIVATE_NETWORK;
import static org.apache.dubbo.rpc.protocol.tri.rest.cors.CorsUtil.getPort;

public class CorsProcessor {
    private static final ErrorTypeAwareLogger LOGGER = LoggerFactory.getErrorTypeAwareLogger(CorsProcessor.class);

    public boolean process(CorsMeta config, HttpRequest request, HttpResponse response) {
        // set vary header
        setVaryHeaders(response);

        // skip if is not a cors request
        if (!isCorsRequest(request)) {
            return true;
        }

        // skip if origin already contains in Access-Control-Allow-Origin header
        if (response.header(RestConstants.ACCESS_CONTROL_ALLOW_ORIGIN) != null) {
            return true;
        }
        boolean preFlight = isPreFlight(request);
        if (config == null) {
            // if no cors config and is a preflight request
            if (preFlight) {
                return reject(response);
            }
            return true;
        }

        // handle cors request
        return handleInternal(request, response, config, preFlight);
    }

    protected boolean handleInternal(HttpRequest request, HttpResponse response, CorsMeta config, boolean isPreLight) {
        String allowOrigin = config.checkOrigin(request.header(RestConstants.ORIGIN));
        if (allowOrigin == null) {
            return reject(response);
        }

        List<HttpMethods> allowHttpMethods = config.checkHttpMethods(getHttpMethods(request, isPreLight));
        if (allowHttpMethods == null) {
            return reject(response);
        }
        List<String> httpHeaders = getHttpHeaders(request, isPreLight);
        List<String> allowHeaders = config.checkHeaders(httpHeaders);
        if (isPreLight && httpHeaders != null && allowHeaders == null) {
            return reject(response);
        }

        response.setHeader(RestConstants.ACCESS_CONTROL_ALLOW_ORIGIN, allowOrigin);

        if (isPreLight) {
            response.setHeader(
                    RestConstants.ACCESS_CONTROL_ALLOW_METHODS,
                    allowHttpMethods.stream().map(Enum::name).collect(Collectors.toList()));
            if (!CollectionUtils.isEmpty(allowHeaders)) {
                response.setHeader(RestConstants.ACCESS_CONTROL_ALLOW_HEADERS, allowHeaders);
            }
            if (config.getMaxAge() != null) {
                response.setHeader(
                        RestConstants.ACCESS_CONTROL_MAX_AGE, config.getMaxAge().toString());
            }
        }

        if (!CollectionUtils.isEmpty(config.getExposedHeaders())) {
            response.setHeader(RestConstants.ACCESS_CONTROL_EXPOSE_HEADERS, config.getExposedHeaders());
        }

        if (Boolean.TRUE.equals(config.getAllowCredentials())) {
            response.setHeader(RestConstants.ACCESS_CONTROL_ALLOW_CREDENTIALS, Boolean.TRUE.toString());
        }

        if (Boolean.TRUE.equals(config.getAllowPrivateNetwork())
                && Boolean.parseBoolean(request.header(ACCESS_CONTROL_REQUEST_PRIVATE_NETWORK))) {
            response.setHeader(ACCESS_CONTROL_ALLOW_PRIVATE_NETWORK, Boolean.TRUE.toString());
        }

        return true;
    }

    private HttpMethods getHttpMethods(HttpRequest request, Boolean isPreLight) {
        if (isPreLight) {
            return HttpMethods.valueOf(request.header(RestConstants.ACCESS_CONTROL_REQUEST_METHOD));
        }
        return HttpMethods.valueOf(request.method());
    }

    private List<String> getHttpHeaders(HttpRequest request, Boolean isPreLight) {
        if (isPreLight) {
            return request.headerValues(RestConstants.ACCESS_CONTROL_REQUEST_HEADERS);
        }
        return new ArrayList<>(request.headerNames());
    }

    private boolean reject(HttpResponse response) {
        response.setStatus(HttpStatus.FORBIDDEN.getCode());
        response.setBody("Invalid CORS request");
        return false;
    }

    public static boolean isPreFlight(HttpRequest request) {
        // preflight request is a OPTIONS request with Access-Control-Request-Method header
        return request.method().equals(HttpMethods.OPTIONS.name())
                && request.header(RestConstants.ACCESS_CONTROL_REQUEST_METHOD) != null;
    }

    private boolean isCorsRequest(HttpRequest request) {
        // skip if request has no origin header
        String origin = request.header(RestConstants.ORIGIN);
        if (origin == null) {
            return false;
        }

        try {
            URI uri = new URI(origin);

            // return true if origin is not the same as request's scheme, host and port
            return !(Objects.equals(uri.getScheme(), request.scheme())
                    && uri.getHost().equals(request.serverName())
                    && getPort(uri.getScheme(), uri.getPort()) == getPort(request.scheme(), request.serverPort()));
        } catch (URISyntaxException e) {
            LOGGER.debug("Origin header is not a valid URI: " + origin);
            // skip if origin is not a valid URI
            return true;
        }
    }

    private void setVaryHeaders(HttpResponse response) {
        List<String> varyHeaders = response.headerValues(RestConstants.VARY);
        if (varyHeaders == null) {
            response.addHeader(RestConstants.VARY, RestConstants.ORIGIN);
            response.addHeader(RestConstants.VARY, RestConstants.ACCESS_CONTROL_REQUEST_METHOD);
            response.addHeader(RestConstants.VARY, RestConstants.ACCESS_CONTROL_REQUEST_HEADERS);
        } else {
            if (!varyHeaders.contains(RestConstants.ORIGIN)) {
                response.addHeader(RestConstants.VARY, RestConstants.ORIGIN);
            }
            if (!varyHeaders.contains(RestConstants.ACCESS_CONTROL_REQUEST_METHOD)) {
                response.addHeader(RestConstants.VARY, RestConstants.ACCESS_CONTROL_REQUEST_METHOD);
            }
            if (!varyHeaders.contains(RestConstants.ACCESS_CONTROL_REQUEST_HEADERS)) {
                response.addHeader(RestConstants.VARY, RestConstants.ACCESS_CONTROL_REQUEST_HEADERS);
            }
        }
    }
}
