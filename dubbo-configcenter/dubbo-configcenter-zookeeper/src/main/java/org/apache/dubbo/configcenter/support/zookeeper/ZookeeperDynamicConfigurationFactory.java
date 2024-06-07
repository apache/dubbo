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
package org.apache.dubbo.configcenter.support.zookeeper;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.AbstractDynamicConfigurationFactory;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.remoting.zookeeper.curator5.ZookeeperClientManager;
import org.apache.dubbo.rpc.model.ApplicationModel;

public class ZookeeperDynamicConfigurationFactory extends AbstractDynamicConfigurationFactory {

    private final ZookeeperClientManager zookeeperClientManager;

    private final ApplicationModel applicationModel;

    public ZookeeperDynamicConfigurationFactory(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
        this.zookeeperClientManager = ZookeeperClientManager.getInstance(applicationModel);
    }

    @Override
    protected DynamicConfiguration createDynamicConfiguration(URL url) {
        return new ZookeeperDynamicConfiguration(url, zookeeperClientManager, applicationModel);
    }
}
