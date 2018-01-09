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

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.governance.sync.RegistryServerSync;
import com.alibaba.dubbo.registry.RegistryService;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * IbatisDAO
 *
 */
public class AbstractService {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractService.class);
    @Autowired
    protected RegistryService registryService;
    @Autowired
    private RegistryServerSync sync;

    public ConcurrentMap<String, ConcurrentMap<String, Map<Long, URL>>> getRegistryCache() {
        return sync.getRegistryCache();
    }


}
