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

package org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice;

import java.util.List;


public class VirtualServiceSpec {
    private List<String> hosts;
    private List<DubboRoute> dubbo;

    public List<String> getHosts() {
        return hosts;
    }

    public void setHosts(List<String> hosts) {
        this.hosts = hosts;
    }

    public List<DubboRoute> getDubbo() {
        return dubbo;
    }

    public void setDubbo(List<DubboRoute> dubbo) {
        this.dubbo = dubbo;
    }

    @Override
    public String toString() {
        return "VirtualServiceSpec{" +
                "hosts=" + hosts +
                ", dubbo=" + dubbo +
                '}';
    }
}
