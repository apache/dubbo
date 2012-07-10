/*
 * Copyright 1999-2011 Alibaba Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.alibaba.dubbo.common.store.support;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.dubbo.common.store.DataStore;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class ThreadNotSafeDataStore implements DataStore {

    // <组件类名或标识, <数据名, 数据值>>
    private Map<String, Map<String, Object>> datas =
        new HashMap<String, Map<String, Object>>();

    @SuppressWarnings("unchecked")
    public Object get(String componentName, String key) {
        if (!datas.containsKey(componentName)) {
            return null;
        }
        return datas.get(componentName).get(key);
    }

    public void put(String componentName, String key, Object value) {
        Map<String, Object> componentDatas = null;
        if (!datas.containsKey(componentName)) {
            componentDatas = new HashMap<String, Object>();
        } else {
            componentDatas = datas.get(componentName);
        }
        componentDatas.put(key, value);
        datas.put(componentName, componentDatas);
    }

    public void remove(String componentName, String key) {
        if (!datas.containsKey(componentName)) {
            return;
        }
        datas.get(componentName).remove(key);
    }

}
