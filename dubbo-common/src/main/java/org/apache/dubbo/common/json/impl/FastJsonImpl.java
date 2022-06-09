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
package org.apache.dubbo.common.json.impl;

import org.apache.dubbo.common.json.JSON;

import com.alibaba.fastjson.JSONArray;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FastJsonImpl implements JSON {

    @Override
    public <T> T toJavaObject(String json, Type type) {
        return com.alibaba.fastjson.JSON.parseObject(json, type);
    }

    @Override
    public <T> Set<T> toJavaSet(String json, Class<T> clazz) {
        JSONArray jsonArray = com.alibaba.fastjson.JSON.parseArray(json);
        Set<T> set = new HashSet<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            set.add(jsonArray.getObject(i, clazz));
        }
        return set;
    }

    @Override
    public <T> List<T> toJavaList(String json, Class<T> clazz) {
        return (List<T>) com.alibaba.fastjson.JSON.parseArray(json, new Type[]{clazz});
    }

    @Override
    public String toJson(Object obj) {
        return com.alibaba.fastjson.JSON.toJSONString(obj);
    }
}
