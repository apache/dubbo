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
package org.apache.dubbo.common.json;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public interface JSON {
    boolean isSupport();

    <T> T toJavaObject(String json, Type type);

    <T> List<T> toJavaList(String json, Class<T> clazz);

    String toJson(Object obj);

    List<?> getList(Map<String, ?> obj, String key);

    List<Map<String, ?>> getListOfObjects(Map<String, ?> obj, String key);

    List<String> getListOfStrings(Map<String, ?> obj, String key);

    Map<String, ?> getObject(Map<String, ?> obj, String key);

    Double getNumberAsDouble(Map<String, ?> obj, String key);

    Integer getNumberAsInteger(Map<String, ?> obj, String key);

    Long getNumberAsLong(Map<String, ?> obj, String key);

    String getString(Map<String, ?> obj, String key);

    List<Map<String, ?>> checkObjectList(List<?> rawList);

    List<String> checkStringList(List<?> rawList);
}
