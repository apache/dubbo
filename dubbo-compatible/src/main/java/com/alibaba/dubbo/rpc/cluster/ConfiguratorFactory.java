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
package com.alibaba.dubbo.rpc.cluster;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.rpc.cluster.Configurator;

@Deprecated
public interface ConfiguratorFactory extends org.apache.dubbo.rpc.cluster.ConfiguratorFactory {
    @Adaptive(CommonConstants.PROTOCOL_KEY)
    com.alibaba.dubbo.rpc.cluster.Configurator getConfigurator(com.alibaba.dubbo.common.URL url);

    @Override
    default Configurator getConfigurator(URL url) {
        return this.getConfigurator(new com.alibaba.dubbo.common.URL(url));
    }
}
