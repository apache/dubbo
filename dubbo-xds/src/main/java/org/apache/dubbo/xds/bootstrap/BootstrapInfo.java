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
package org.apache.dubbo.xds.bootstrap;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAlias;

public class BootstrapInfo {
    @JsonAlias("xds_servers")
    private List<XdsServer> xdsServers;

    private Node node;

    @JsonAlias("cert_providers")
    private Map<String, CertificateProvider> certProviders;

    @JsonAlias("server_listener_resource_name_template")
    private String serverListenerResourceNameTemplate;

    public List<XdsServer> getXdsServers() {
        return xdsServers;
    }

    public BootstrapInfo setXdsServers(List<XdsServer> xdsServers) {
        this.xdsServers = xdsServers;
        return this;
    }

    public Node getNode() {
        return node;
    }

    public BootstrapInfo setNode(Node node) {
        this.node = node;
        return this;
    }

    public Map<String, CertificateProvider> getCertProviders() {
        return certProviders;
    }

    public BootstrapInfo setCertProviders(Map<String, CertificateProvider> certProviders) {
        this.certProviders = certProviders;
        return this;
    }

    public String getServerListenerResourceNameTemplate() {
        return serverListenerResourceNameTemplate;
    }

    public BootstrapInfo setServerListenerResourceNameTemplate(String serverListenerResourceNameTemplate) {
        this.serverListenerResourceNameTemplate = serverListenerResourceNameTemplate;
        return this;
    }
}
