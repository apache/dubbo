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
package com.alibaba.dubbo.governance.service.impl;

import com.alibaba.dubbo.governance.service.ConfigService;
import com.alibaba.dubbo.registry.common.domain.Config;

import java.util.List;
import java.util.Map;

/**
 * TODO Comment of IbatisConfigDAO
 *
 */
public class ConfigServiceImpl extends AbstractService implements ConfigService {

    /* (non-Javadoc)
     * @see com.alibaba.dubbo.governance.service.ConfigService#update(java.util.List)
     */
    public void update(List<Config> configs) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.alibaba.dubbo.governance.service.ConfigService#findAllConfigsMap()
     */
    public Map<String, String> findAllConfigsMap() {
        // TODO Auto-generated method stub
        return null;
    }
}
