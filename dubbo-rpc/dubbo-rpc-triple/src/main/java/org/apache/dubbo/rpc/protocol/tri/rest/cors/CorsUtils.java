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

import org.apache.dubbo.config.nested.CorsConfig;
import org.apache.dubbo.config.nested.RestConfig;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.CorsMeta;

public class CorsUtils {

    private CorsUtils() {}

    public static CorsMeta getGlobalCorsMeta(RestConfig restConfig) {
        CorsConfig config = restConfig.getCorsOrDefault();
        return CorsMeta.builder()
                .allowedOrigins(config.getAllowedOrigins())
                .allowedMethods(config.getAllowedMethods())
                .allowedHeaders(config.getAllowedHeaders())
                .allowCredentials(config.getAllowCredentials())
                .exposedHeaders(config.getExposedHeaders())
                .maxAge(config.getMaxAge())
                .build();
    }

    public static String formatOrigin(String value) {
        value = value.trim();
        int last = value.length() - 1;
        return last > -1 && value.charAt(last) == '/' ? value.substring(0, last) : value;
    }
}
