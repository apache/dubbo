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
package org.apache.dubbo.registry.xds.util;

import io.envoyproxy.envoy.config.core.v3.Node;

public class NodeBuilder {
    public static Node build() {
        // TODO: fetch data from environment
        return Node.newBuilder()
                .setId("sidecar~127.0.0.1~ratings-v1-7dc98c7588-lwvqd.default~default.svc.cluster.local")
                .setCluster("ratings.default")
                .build();
    }
}
