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

import com.alibaba.fastjson.serializer.SerializerFeature;

import java.lang.reflect.Type;
import java.util.List;

public class FastJsonImpl extends AbstractJSONImpl {

    @Override
    public <T> T toJavaObject(String json, Type type) {
        return com.alibaba.fastjson.JSON.parseObject(json, type);
    }

    @Override
    public <T> List<T> toJavaList(String json, Class<T> clazz) {
        return com.alibaba.fastjson.JSON.parseArray(json, clazz);
    }

    @Override
    public String toJson(Object obj) {
        return com.alibaba.fastjson.JSON.toJSONString(obj, SerializerFeature.DisableCircularReferenceDetect);
    }

}
