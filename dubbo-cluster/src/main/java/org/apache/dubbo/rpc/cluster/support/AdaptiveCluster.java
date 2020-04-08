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
package org.apache.dubbo.rpc.cluster.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Cluster;
import org.apache.dubbo.rpc.cluster.Directory;

@Adaptive
public class AdaptiveCluster implements Cluster {

    @Override
    public <T> Invoker<T> join(Directory<T> directory) throws RpcException {
        if (directory == null) {
            throw new RpcException("Cluster join fail. Cause: directory is null !");
        }

        // use ConsumerUrl first
        URL url = directory.getConsumerUrl();
        url = url != null ? url : directory.getUrl();
        String extensionName = url != null ? url.getParameter(CommonConstants.CLUSTER_KEY) : null;
        if (extensionName != null) {
            return ExtensionLoader.getExtensionLoader(Cluster.class).getExtension(extensionName).join(directory);
        } else {
            return ExtensionLoader.getExtensionLoader(Cluster.class).getDefaultExtension().join(directory);
        }
    }
}
