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
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.apache.dubbo.rpc.protocol.tri.rest.RestConstants.CONFIG_PREFIX;
import static org.apache.dubbo.rpc.protocol.tri.rest.RestConstants.CORS_CONFIG_PREFIX;

public class CorsUtil {

    public static int getPort(String scheme, int port) {
        if (port == -1) {
            if (RestConstants.HTTP.equals(scheme) || RestConstants.WS.equals(scheme)) {
                port = 80;
            } else if (RestConstants.HTTPS.equals(scheme) || RestConstants.WSS.equals(scheme)) {
                port = 443;
            }
        }
        return port;
    }

    public static CorsMeta resolveGlobalMeta(Configuration config) {
        // Get the CORS configuration properties from the configuration object.
        String allowOrigins = config.getString(CORS_CONFIG_PREFIX+RestConstants.ACCESS_CONTROL_ALLOW_ORIGIN);
        String allowMethods = config.getString(CORS_CONFIG_PREFIX+RestConstants.ACCESS_CONTROL_ALLOW_METHODS);
        String allowHeaders = config.getString(CORS_CONFIG_PREFIX+RestConstants.ACCESS_CONTROL_ALLOW_HEADERS);
        String exposeHeaders = config.getString(CORS_CONFIG_PREFIX+RestConstants.ACCESS_CONTROL_EXPOSE_HEADERS);
        String maxAge = config.getString(CORS_CONFIG_PREFIX+RestConstants.ACCESS_CONTROL_MAX_AGE);
        String allowCredentials = config.getString(CORS_CONFIG_PREFIX+RestConstants.ACCESS_CONTROL_ALLOW_CREDENTIALS);
        String allowPrivateNetwork = config.getString(CORS_CONFIG_PREFIX+RestConstants.ACCESS_CONTROL_ALLOW_PRIVATE_NETWORK);
        // Create a new CorsMeta object and set the properties.
        CorsMeta meta = new CorsMeta();
        meta.setAllowedOrigins(parseList(allowOrigins));
        meta.setAllowedMethods(parseList(allowMethods));
        meta.setAllowedHeaders(parseList(allowHeaders));
        meta.setExposedHeaders(parseList(exposeHeaders));
        meta.setMaxAge( maxAge == null ? null : Long.getLong(maxAge) );
        meta.setAllowCredentials(allowCredentials == null ?null : Boolean.getBoolean(allowCredentials));
        meta.setAllowPrivateNetwork( allowPrivateNetwork == null ?null : Boolean.getBoolean(allowPrivateNetwork));
        // Return the CorsMeta object.
        return meta;
    }

    private static List<String> parseList(String value) {
        if (value == null) {
            return null;
        }

        List<String> list = new ArrayList<>();
        for (String item : value.split(",")) {
            list.add(item.trim());
        }

        return list;
    }
}
