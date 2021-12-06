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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.config.nested.AggregationConfig;
import org.apache.dubbo.config.nested.PrometheusConfig;
import org.apache.dubbo.config.support.Nested;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.PROTOCOL_PROMETHEUS;

/**
 * MetricsConfig
 */
public class MetricsConfig extends AbstractConfig {

    private static final long serialVersionUID = -9089919311611546383L;

    private String protocol;

    /**
     * @deprecated After metrics config is refactored.
     * This parameter should no longer use and will be deleted in the future.
     */
    @Deprecated
    private String port;

    /**
     * The prometheus metrics config
     */
    @Nested
    private PrometheusConfig prometheus;

    /**
     * The metrics aggregation config
     */
    @Nested
    private AggregationConfig aggregation;

    public MetricsConfig() {
    }

    public MetricsConfig(ApplicationModel applicationModel) {
        super(applicationModel);
    }

    public URL toUrl() {
        Map<String, String> map = new HashMap<>();
        appendParameters(map, this);

        // use 'prometheus' as the default metrics service.
        if (StringUtils.isEmpty(map.get(PROTOCOL_KEY))) {
            map.put(PROTOCOL_KEY, PROTOCOL_PROMETHEUS);
        }

        // ignore address parameter, use specified url in each metrics server config
        // the address "localhost" here is meaningless
        return UrlUtils.parseURL("localhost", map);
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public PrometheusConfig getPrometheus() {
        return prometheus;
    }

    public void setPrometheus(PrometheusConfig prometheus) {
        this.prometheus = prometheus;
    }

    public AggregationConfig getAggregation() {
        return aggregation;
    }

    public void setAggregation(AggregationConfig aggregation) {
        this.aggregation = aggregation;
    }
}

