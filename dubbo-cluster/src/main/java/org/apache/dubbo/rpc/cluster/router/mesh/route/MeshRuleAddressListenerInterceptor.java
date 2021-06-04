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
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.registry.AddressListener;
import org.apache.dubbo.rpc.cluster.Directory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Activate(order = 670)
public class MeshRuleAddressListenerInterceptor implements AddressListener {

    private static final Object MARK = new Object();
    private static final ConcurrentHashMap<String, Object> APP_MAP = new ConcurrentHashMap<>();

    @Override
    public List<URL> notify(List<URL> addresses, URL consumerUrl, Directory registryDirectory) {

        if (addresses != null && !addresses.isEmpty()) {
            for (URL url : addresses) {

                String app = url.getRemoteApplication();
                if (app != null && !app.isEmpty()) {
                    if (APP_MAP.putIfAbsent(app, MARK) == null) {
                        MeshRuleManager.subscribeAppRule(app);
                    }
                }
            }
        }

        return addresses;
    }
}
