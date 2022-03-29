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
package org.apache.dubbo.qos.command.impl;

import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.qos.command.annotation.Cmd;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ProviderModel;

@Cmd(name = "offlineApp", summary = "offline app addresses", example = {
        "offlineApp",
        "offlineApp xx.xx.xxx.service"
})
public class OfflineApp extends BaseOffline {

    public OfflineApp(FrameworkModel frameworkModel) {
        super(frameworkModel);
    }

    @Override
    protected void doUnexport(ProviderModel.RegisterStatedURL statedURL) {
        if (UrlUtils.isServiceDiscoveryURL(statedURL.getRegistryUrl())) {
            super.doUnexport(statedURL);
        }
    }
}
