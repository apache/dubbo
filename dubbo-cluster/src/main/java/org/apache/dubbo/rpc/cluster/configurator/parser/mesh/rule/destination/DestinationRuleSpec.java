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
package org.apache.dubbo.rpc.cluster.configurator.parser.mesh.rule.destination;

import java.util.List;

public class DestinationRuleSpec {
    private String host;
    private List<Subset> subsets;
    private TrafficPolicy trafficPolicy;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public List<Subset> getSubsets() {
        return subsets;
    }

    public void setSubsets(List<Subset> subsets) {
        this.subsets = subsets;
    }

    public TrafficPolicy getTrafficPolicy() {
        return trafficPolicy;
    }

    public void setTrafficPolicy(TrafficPolicy trafficPolicy) {
        this.trafficPolicy = trafficPolicy;
    }

    @Override
    public String toString() {
        return "DestinationRuleSpec{" + "host='"
                + host + '\'' + ", subsets="
                + subsets + ", trafficPolicy="
                + trafficPolicy + '}';
    }
}