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
package org.apache.dubbo.rpc.cluster.router.xds.rule;

import io.envoyproxy.envoy.config.route.v3.VirtualHost;

import java.util.Map;
import java.util.Set;

/**
 * @author : MentosL
 * @date : 2022/12/11 21:29
 */
public class XdsDomainResult {

    private Map<String, Set<String>> map;
    private Map<String, VirtualHost> rdsVirtualhostMap;

    public XdsDomainResult() {
    }

    public XdsDomainResult(Map<String, Set<String>> map, Map<String, VirtualHost> rdsVirtualhostMap) {
        this.map = map;
        this.rdsVirtualhostMap = rdsVirtualhostMap;
    }

    public Map<String, Set<String>> getMap() {
        return map;
    }

    public void setMap(Map<String, Set<String>> map) {
        this.map = map;
    }

    public Map<String, VirtualHost> getRdsVirtualhostMap() {
        return rdsVirtualhostMap;
    }

    public void setRdsVirtualhostMap(Map<String, VirtualHost> rdsVirtualhostMap) {
        this.rdsVirtualhostMap = rdsVirtualhostMap;
    }
}
