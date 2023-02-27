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
package org.apache.dubbo.config.nested;

import org.apache.dubbo.config.support.Nested;

public class PrometheusConfig {

    /**
     * Prometheus exporter configuration
     */
    @Nested
    private Exporter exporter;

    /**
     * Prometheus Pushgateway configuration
     */
    @Nested
    private Pushgateway pushgateway;

    public Exporter getExporter() {
        return exporter;
    }

    public void setExporter(Exporter exporter) {
        this.exporter = exporter;
    }

    public Pushgateway getPushgateway() {
        return pushgateway;
    }

    public void setPushgateway(Pushgateway pushgateway) {
        this.pushgateway = pushgateway;
    }

    public static class Exporter {

        /**
         * Enable prometheus exporter
         */
        private Boolean enabled;

        /**
         * Enable http service discovery for prometheus
         */
        private Boolean enableHttpServiceDiscovery;

        /**
         * Http service discovery url
         */
        private String httpServiceDiscoveryUrl;

        /**
         * When using pull method, which port to expose
         */
        private Integer metricsPort;

        /**
         * When using pull mode, which path to expose metrics
         */
        private String metricsPath;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public Boolean getEnableHttpServiceDiscovery() {
            return enableHttpServiceDiscovery;
        }

        public void setEnableHttpServiceDiscovery(Boolean enableHttpServiceDiscovery) {
            this.enableHttpServiceDiscovery = enableHttpServiceDiscovery;
        }

        public String getHttpServiceDiscoveryUrl() {
            return httpServiceDiscoveryUrl;
        }

        public void setHttpServiceDiscoveryUrl(String httpServiceDiscoveryUrl) {
            this.httpServiceDiscoveryUrl = httpServiceDiscoveryUrl;
        }

        public Integer getMetricsPort() {
            return metricsPort;
        }

        public void setMetricsPort(Integer metricsPort) {
            this.metricsPort = metricsPort;
        }

        public String getMetricsPath() {
            return metricsPath;
        }

        public void setMetricsPath(String metricsPath) {
            this.metricsPath = metricsPath;
        }
    }

    public static class Pushgateway {

        /**
         * Enable publishing via a Prometheus Pushgateway
         */
        private Boolean enabled;

        /**
         * Base URL for the Pushgateway
         */
        private String baseUrl;

        /**
         * Login user of the Prometheus Pushgateway
         */
        private String username;

        /**
         * Login password of the Prometheus Pushgateway
         */
        private String password;

        /**
         * Frequency with which to push metrics
         */
        private Integer pushInterval;

        /**
         * Job identifier for this application instance
         */
        private String job;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public Integer getPushInterval() {
            return pushInterval;
        }

        public void setPushInterval(Integer pushInterval) {
            this.pushInterval = pushInterval;
        }

        public String getJob() {
            return job;
        }

        public void setJob(String job) {
            this.job = job;
        }
    }
}
