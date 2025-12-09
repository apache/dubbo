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

public class Node {
    private String id;
    private String cluster;
    private Map<String, ?> metadata;
    private Locality locality;
    private String userAgentName;
    private String userAgentVersion;
    private List<String> clientFeatures;

    public String getId() {
        return id;
    }

    public Node setId(String id) {
        this.id = id;
        return this;
    }

    public String getCluster() {
        return cluster;
    }

    public Node setCluster(String cluster) {
        this.cluster = cluster;
        return this;
    }

    public Map<String, ?> getMetadata() {
        return metadata;
    }

    public Node setMetadata(Map<String, ?> metadata) {
        this.metadata = metadata;
        return this;
    }

    public Locality getLocality() {
        return locality;
    }

    public Node setLocality(Locality locality) {
        this.locality = locality;
        return this;
    }

    public String getUserAgentName() {
        return userAgentName;
    }

    public Node setUserAgentName(String userAgentName) {
        this.userAgentName = userAgentName;
        return this;
    }

    public String getUserAgentVersion() {
        return userAgentVersion;
    }

    public Node setUserAgentVersion(String userAgentVersion) {
        this.userAgentVersion = userAgentVersion;
        return this;
    }

    public List<String> getClientFeatures() {
        return clientFeatures;
    }

    public Node setClientFeatures(List<String> clientFeatures) {
        this.clientFeatures = clientFeatures;
        return this;
    }
}
