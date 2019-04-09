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

package org.apache.dubbo.config;

import org.apache.dubbo.common.Constants;

import java.util.Map;

public class MetricsConfig extends AbstractConfig {

    private static final long serialVersionUID = -9089919311611546383L;

    private static String port;
    private static String protocol;

    public static String getPort() {
        return port;
    }

    public static void setPort(String port) {
        MetricsConfig.port = port;
    }

    public static String getProtocol() {
        return protocol;
    }

    public static void setProtocol(String protocol) {
        MetricsConfig.protocol = protocol;
    }

    public static void addMetricsDataToMap(Map map) {
        if(MetricsConfig.getPort() != null && MetricsConfig.getProtocol() != null) {
            map.put(Constants.METRICS_PORT, MetricsConfig.getPort());
            map.put(Constants.METRICS_PROTOCOL, MetricsConfig.getProtocol());
        }
    }
}
