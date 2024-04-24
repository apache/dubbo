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

import org.apache.dubbo.common.config.Configuration;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants;

import java.util.Arrays;

public class CorsUtils {
    private static CorsMeta globalCorsMeta;
    public static final String HTTP = "http";
    public static final String HTTPS = "https";
    public static final String WS = "ws";
    public static final String WSS = "wss";

    private CorsUtils() {}

    public static int getPort(String scheme, int port) {
        if (port == -1) {
            if (HTTP.equals(scheme) || WS.equals(scheme)) {
                port = 80;
            } else if (HTTPS.equals(scheme) || WSS.equals(scheme)) {
                port = 443;
            }
        }
        return port;
    }

    public static CorsMeta resolveGlobalMeta(Configuration config) {
        // Get the CORS configuration properties from the configuration object.
        String[] allowOrigins = config.convert(String[].class, RestConstants.ALLOWED_ORIGINS, null);
        String[] allowMethods = config.convert(String[].class, RestConstants.ALLOWED_METHODS, null);
        String[] allowHeaders = config.convert(String[].class, RestConstants.ALLOWED_HEADERS, null);
        String[] exposeHeaders = config.convert(String[].class, RestConstants.EXPOSED_HEADERS, null);
        Object maxAge = config.getProperty(RestConstants.MAX_AGE);
        // Create a new CorsMeta object and set the properties.
        CorsMeta meta = new CorsMeta();
        meta.setAllowedOrigins(allowOrigins == null?null:Arrays.asList(allowOrigins));
        meta.setAllowedMethods(allowMethods == null?null:Arrays.asList(allowMethods));
        meta.setAllowedHeaders(allowHeaders == null?null:Arrays.asList(allowHeaders));
        meta.setExposedHeaders(exposeHeaders == null?null:Arrays.asList(exposeHeaders));
        meta.setMaxAge(maxAge == null ? null : (Long) maxAge);
        // Return the CorsMeta object.
        return meta.applyPermitDefaultValues();
    }

    public static CorsMeta getGlobalCorsMeta() {
        if (globalCorsMeta == null) {
            Configuration globalConfiguration =
                    ConfigurationUtils.getGlobalConfiguration(ApplicationModel.defaultModel());
            globalCorsMeta = resolveGlobalMeta(globalConfiguration);
        }
        return globalCorsMeta;
    }
}
