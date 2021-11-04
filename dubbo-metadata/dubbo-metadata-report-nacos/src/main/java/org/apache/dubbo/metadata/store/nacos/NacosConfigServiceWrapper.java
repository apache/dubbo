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
package org.apache.dubbo.metadata.store.nacos;

import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;

import static org.apache.dubbo.common.utils.StringUtils.HYPHEN_CHAR;
import static org.apache.dubbo.common.utils.StringUtils.SLASH_CHAR;

public class NacosConfigServiceWrapper {

    private static final String INNERCLASS_SYMBOL = "$";

    private static final String INNERCLASS_COMPATIBLE_SYMBOL = "___";

    private static final long DEFAULT_TIMEOUT = 3000L;

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

    public void removeListener(String dataId, String group, Listener listener) throws NacosException {
        configService.removeListener(handleInnerSymbol(dataId), handleInnerSymbol(group), listener);
    }

    public String getConfig(String dataId, String group) throws NacosException {
        return configService.getConfig(handleInnerSymbol(dataId), handleInnerSymbol(group), DEFAULT_TIMEOUT);
    }

    public String getConfig(String dataId, String group, long timeout) throws NacosException {
        return configService.getConfig(handleInnerSymbol(dataId), handleInnerSymbol(group), timeout);
    }

    public boolean publishConfig(String dataId, String group, String content) throws NacosException {
        return configService.publishConfig(handleInnerSymbol(dataId), handleInnerSymbol(group), content);
    }

    public boolean publishConfigCas(String dataId, String group, String content, String casMd5) throws NacosException {
        return configService.publishConfigCas(handleInnerSymbol(dataId), handleInnerSymbol(group), content, casMd5);
    }

    public boolean removeConfig(String dataId, String group) throws NacosException {
        return configService.removeConfig(handleInnerSymbol(dataId), handleInnerSymbol(group));
    }

    /**
     * see {@link com.alibaba.nacos.client.config.utils.ParamUtils#isValid(java.lang.String)}
     */
    private String handleInnerSymbol(String data) {
        if (data == null) {
            return null;
        }
        return data.replace(INNERCLASS_SYMBOL, INNERCLASS_COMPATIBLE_SYMBOL).replace(SLASH_CHAR, HYPHEN_CHAR);
    }

}
