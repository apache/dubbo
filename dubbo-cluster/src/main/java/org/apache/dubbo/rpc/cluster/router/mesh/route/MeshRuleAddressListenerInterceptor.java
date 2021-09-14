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
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.registry.AddressListener;
import org.apache.dubbo.rpc.cluster.Directory;

import java.util.List;
import java.util.Set;

@Activate(order = 670)
public class MeshRuleAddressListenerInterceptor implements AddressListener {

    private static final Set<String> APP_SET = new ConcurrentHashSet<>();

    @Override
    public List<URL> notify(List<URL> addresses, URL consumerUrl, Directory registryDirectory) {

        if (CollectionUtils.isNotEmpty(addresses)) {
            for (URL url : addresses) {

                String app = url.getRemoteApplication();
                if (StringUtils.isNotEmpty(app)) {
                    if (APP_SET.add(app)) {
                        MeshRuleManager.subscribeAppRule(consumerUrl, app);
                    }
                }
            }
        }

        return addresses;
    }

    @Override
    public void destroy(URL consumerUrl, Directory registryDirectory) {
        for (String app : APP_SET) {
            MeshRuleManager.unsubscribeAppRule(consumerUrl, app);
        }
    }
}
