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

import com.alibaba.fastjson.serializer.SerializeFilter;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonContainer;
import com.alibaba.fastjson.support.spring.PropertyPreFilters;
import org.apache.dubbo.common.utils.ClassUtils;

import com.alibaba.fastjson2.JSONWriter;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FastJson2Impl extends AbstractJSONImpl {
    private FastJsonConfig fastJsonConfig = new FastJsonConfig();

    @Override
    public boolean isSupport() {
        try {
            Class<?> aClass = ClassUtils.forName("com.alibaba.fastjson2.JSON");
            return aClass != null;
        } catch (Exception t) {
            return false;
        }
    }

    @Override
    public <T> T toJavaObject(String json, Type type) {
        return com.alibaba.fastjson2.JSON.parseObject(json, type);
    }

    @Override
    public <T> List<T> toJavaList(String json, Class<T> clazz) {
        return com.alibaba.fastjson2.JSON.parseArray(json, clazz);
    }

    @Override
    public String toJson(Object obj) {
        return com.alibaba.fastjson2.JSON.toJSONString(obj, JSONWriter.Feature.WriteEnumsUsingName);
    }

    @Override
    public <T> T parseObject(byte[] bytes, Class<T> clazz) {
        return com.alibaba.fastjson2.JSON.parseObject(bytes, clazz);
    }

    @Override
    public <T> T parseObject(InputStream inputStream, Class<T> clazz) {
        return com.alibaba.fastjson2.JSON.parseObject(inputStream, fastJsonConfig.getCharset(), clazz);

    }

    @Override
    public <T> void serializeObject(OutputStream outputStream, Object value) throws Exception {
        ByteArrayOutputStream outnew = new ByteArrayOutputStream();
        SerializeFilter[] globalFilters = this.fastJsonConfig.getSerializeFilters();
        List<SerializeFilter> allFilters = new ArrayList(Arrays.asList(globalFilters));


        if (value instanceof FastJsonContainer) {
            FastJsonContainer fastJsonContainer = (FastJsonContainer) value;
            PropertyPreFilters filters = fastJsonContainer.getFilters();
            allFilters.addAll(filters.getFilters());
            value = fastJsonContainer.getValue();
        }
        com.alibaba.fastjson2.JSON.writeTo(outnew, value);


        outnew.writeTo(outputStream);
    }
}

