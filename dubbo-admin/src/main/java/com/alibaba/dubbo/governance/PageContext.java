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
package com.alibaba.dubbo.governance;

import com.alibaba.dubbo.registry.common.domain.User;

import java.util.Map;

/**
 * Context
 *
 */
public interface PageContext {

    public String get(String key);

    public String[] gets(String key);

    public Map<String, String[]> getAll();

    public void put(String key, Object value);

    public String getMessage(String key, Object... args);

    public String getClientAddress();

    public String getOperateAddress();

    public String getRegistryAddress();

    public String getURI();

    public String getURL();

    public String getReferer();

    public User getLoginUser();

}
