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
package org.apache.dubbo.configcenter.support.nacos;

import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;

public class NacosConfigServiceWrapper {

    private static final String INNERCLASS_SYMBOL = "$";

    private static final String INNERCLASS_COMPATIBLE_SYMBOL = "___";

    private ConfigService configService;


    public NacosConfigServiceWrapper(ConfigService configService) {
        this.configService = configService;
    }

    public ConfigService getConfigService() {
        return configService;
    }

    public void addListener(String dataId, String group, Listener listener) throws NacosException {
        configService.addListener(handleInnerSymbol(dataId), handleInnerSymbol(group), listener);
    }

    public String getConfig(String dataId, String group, long timeout) throws NacosException {
        return configService.getConfig(handleInnerSymbol(dataId), handleInnerSymbol(group), timeout);
    }

    public boolean publishConfig(String dataId, String group, String content) throws NacosException {
        return configService.publishConfig(handleInnerSymbol(dataId), handleInnerSymbol(group), content);
    }

    public boolean removeConfig(String dataId, String group) throws NacosException {
        return configService.removeConfig(handleInnerSymbol(dataId), handleInnerSymbol(group));
    }

    /**
     * see {@link com.alibaba.nacos.client.config.utils.ParamUtils#isValid(java.lang.String)}
     */
    private String handleInnerSymbol(String dataId) {
        if (dataId == null) {
            return null;
        }
        return dataId.replace(INNERCLASS_SYMBOL, INNERCLASS_COMPATIBLE_SYMBOL);
    }
}
