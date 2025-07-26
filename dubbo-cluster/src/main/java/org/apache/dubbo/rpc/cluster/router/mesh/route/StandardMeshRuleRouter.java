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
package org.apache.dubbo.rpc.cluster.router.mesh.route;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.router.RouterSnapshotNode;
import org.apache.dubbo.rpc.cluster.router.state.BitList;
import org.apache.dubbo.common.utils.Holder;

import static org.apache.dubbo.rpc.cluster.router.mesh.route.MeshRuleConstants.STANDARD_ROUTER_KEY;

public class StandardMeshRuleRouter<T> extends MeshRuleRouter<T> {

    private final static ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(StandardMeshRuleRouter.class);

    public StandardMeshRuleRouter(URL url) {
        super(url);
    }

    @Override
    public String ruleSuffix() {
        return STANDARD_ROUTER_KEY;
    }


}
