/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.rpc.protocol.tri.rest.cors;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.remoting.http12.HttpResult;
import org.apache.dubbo.remoting.http12.HttpStatus;
import org.apache.dubbo.remoting.http12.exception.HttpResultPayloadException;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants;
import org.apache.dubbo.rpc.protocol.tri.rest.filter.RestHeaderFilterAdapter;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RequestMapping;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.CorsMeta;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.apache.dubbo.common.constants.CommonConstants.ANY_VALUE;

/**
 * See: <a href="https://github.com/spring-projects/spring-framework/blob/main/spring-web/src/main/java/org/springframework/web/cors/DefaultCorsProcessor.java">DefaultCorsProcessor</a>
 */
@Activate(group = CommonConstants.PROVIDER, order = 1000)
public class CorsHeaderFilter extends RestHeaderFilterAdapter {

    public static final String VARY = "Vary";
    public static final String ORIGIN = "Origin";
    public static final String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";
    public static final String ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";
    public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    public static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
    public static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";
    public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
    public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
    public static final String SEP = ", ";

    @Override
    protected void invoke(Invoker<?> invoker, RpcInvocation invocation, HttpRequest request, HttpResponse response)
            throws RpcException {
        RequestMapping mapping = request.attribute(RestConstants.MAPPING_ATTRIBUTE);
        CorsMeta cors = mapping.getCors();
        String origin = request.header(ORIGIN);
        if (cors == null) {
            if (isPreFlightRequest(request, origin)) {
                throw new HttpResultPayloadException(HttpResult.builder()
                        .status(HttpStatus.FORBIDDEN)
                        .body("Invalid CORS request")
                        .build());
            }
            return;
        }

        if (process(cors, request, response)) {
            return;
        }

        throw new HttpResultPayloadException(HttpResult.builder()
                .status(HttpStatus.FORBIDDEN)
                .body("Invalid CORS request")
                .headers(response.headers())
                .build());
    }

    private boolean process(CorsMeta cors, HttpRequest request, HttpResponse response) {
        setVaryHeader(response);

        String origin = request.header(ORIGIN);
        if (isNotCorsRequest(request, origin)) {
            return true;
        }

        if (response.header(ACCESS_CONTROL_ALLOW_ORIGIN) != null) {
            return true;
        }

        String allowOrigin = checkOrigin(cors, origin);
        if (allowOrigin == null) {
            return false;
        }

        boolean preFlight = isPreFlightRequest(request, origin);

        List<String> allowMethods =
                checkMethods(cors, preFlight ? request.header(ACCESS_CONTROL_REQUEST_METHOD) : request.method());
        if (allowMethods == null) {
            return false;
        }

        List<String> allowHeaders = null;
        if (preFlight) {
            allowHeaders = checkHeaders(cors, request.headerValues(ACCESS_CONTROL_REQUEST_HEADERS));
            if (allowHeaders == null) {
                return false;
            }
        }

        response.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN, allowOrigin);

        if (ArrayUtils.isNotEmpty(cors.getExposedHeaders())) {
            response.setHeader(ACCESS_CONTROL_EXPOSE_HEADERS, StringUtils.join(cors.getExposedHeaders(), SEP));
        }

        if (Boolean.TRUE.equals(cors.getAllowCredentials())) {
            response.setHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, Boolean.TRUE.toString());
        }

        if (preFlight) {
            response.setHeader(ACCESS_CONTROL_ALLOW_METHODS, StringUtils.join(allowMethods, SEP));

            if (!allowHeaders.isEmpty()) {
                response.setHeader(ACCESS_CONTROL_ALLOW_HEADERS, StringUtils.join(allowHeaders, SEP));
            }
            if (cors.getMaxAge() != null) {
                response.setHeader(ACCESS_CONTROL_MAX_AGE, cors.getMaxAge().toString());
            }
            throw new HttpResultPayloadException(HttpResult.builder()
                    .status(HttpStatus.NO_CONTENT)
                    .headers(response.headers())
                    .build());
        }

        return true;
    }

    private static void setVaryHeader(HttpResponse response) {
        List<String> varyHeaders = response.headerValues(VARY);
        String varyValue;
        if (varyHeaders == null) {
            varyValue = ORIGIN + SEP + ACCESS_CONTROL_REQUEST_METHOD + SEP + ACCESS_CONTROL_REQUEST_HEADERS;
        } else {
            Set<String> varHeadersSet = new LinkedHashSet<>(varyHeaders);
            varHeadersSet.add(ORIGIN);
            varHeadersSet.add(ACCESS_CONTROL_REQUEST_METHOD);
            varHeadersSet.add(ACCESS_CONTROL_REQUEST_HEADERS);
            varyValue = StringUtils.join(varHeadersSet, SEP);
        }
        response.setHeader(VARY, varyValue);
    }

    private static String checkOrigin(CorsMeta cors, String origin) {
        if (StringUtils.isBlank(origin)) {
            return null;
        }
        origin = CorsUtils.formatOrigin(origin);
        String[] allowedOrigins = cors.getAllowedOrigins();
        if (ArrayUtils.isNotEmpty(allowedOrigins)) {
            if (ArrayUtils.contains(allowedOrigins, ANY_VALUE)) {
                if (Boolean.TRUE.equals(cors.getAllowCredentials())) {
                    throw new IllegalArgumentException(
                            "When allowCredentials is true, allowedOrigins cannot contain the special value \"*\"");
                }
                return ANY_VALUE;
            }
            for (String allowedOrigin : allowedOrigins) {
                if (origin.equalsIgnoreCase(allowedOrigin)) {
                    return origin;
                }
            }
        }
        if (ArrayUtils.isNotEmpty(cors.getAllowedOriginsPatterns())) {
            for (Pattern pattern : cors.getAllowedOriginsPatterns()) {
                if (pattern.matcher(origin).matches()) {
                    return origin;
                }
            }
        }
        return null;
    }

    private static List<String> checkMethods(CorsMeta cors, String method) {
        if (method == null) {
            return null;
        }
        String[] allowedMethods = cors.getAllowedMethods();
        if (ArrayUtils.contains(allowedMethods, ANY_VALUE)) {
            return Collections.singletonList(method);
        }
        for (String allowedMethod : allowedMethods) {
            if (method.equalsIgnoreCase(allowedMethod)) {
                return Arrays.asList(allowedMethods);
            }
        }
        return null;
    }

    private static List<String> checkHeaders(CorsMeta cors, Collection<String> headers) {
        if (headers == null || headers.isEmpty()) {
            return Collections.emptyList();
        }
        String[] allowedHeaders = cors.getAllowedHeaders();
        if (ArrayUtils.isEmpty(allowedHeaders)) {
            return null;
        }

        boolean allowAny = ArrayUtils.contains(allowedHeaders, ANY_VALUE);
        List<String> result = new ArrayList<>(headers.size());
        for (String header : headers) {
            if (allowAny) {
                result.add(header);
                continue;
            }
            for (String allowedHeader : allowedHeaders) {
                if (header.equalsIgnoreCase(allowedHeader)) {
                    result.add(header);
                    break;
                }
            }
        }
        return result.isEmpty() ? null : result;
    }

    private static boolean isNotCorsRequest(HttpRequest request, String origin) {
        if (origin == null) {
            return true;
        }
        try {
            URI uri = new URI(origin);
            return request.scheme().equals(uri.getScheme())
                    && request.serverName().equals(uri.getHost())
                    && getPort(request.scheme(), request.serverPort()) == getPort(uri.getScheme(), uri.getPort());
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private static boolean isPreFlightRequest(HttpRequest request, String origin) {
        return request.method().equals(HttpMethods.OPTIONS.name())
                && origin != null
                && request.hasHeader(ACCESS_CONTROL_REQUEST_METHOD);
    }

    private static int getPort(String scheme, int port) {
        if (port == -1) {
            if ("http".equals(scheme)) {
                return 80;
            }
            if ("https".equals(scheme)) {
                return 443;
            }
        }
        return port;
    }
}
