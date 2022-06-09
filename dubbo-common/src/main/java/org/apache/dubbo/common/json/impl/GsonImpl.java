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
//package org.apache.dubbo.common.json.impl;
//
//import org.apache.dubbo.common.json.JSON;
//
//import com.google.gson.Gson;
//import com.google.gson.reflect.TypeToken;
//
//import java.lang.reflect.Type;
//import java.util.List;
//
//public class GsonImpl implements JSON {
//    private Gson gson = new Gson();
//
//    @Override
//    public <T> T toJavaObject(String json) {
//        return (T) gson.fromJson(json, Object.class);
//    }
//
//    @Override
//    public <T> T toJavaObject(String json, Type type) {
//        return gson.fromJson(json, type);
//    }
//
//    @Override
//    public <T> List<T> toJavaList(String json) {
//        return gson.fromJson(json, List.class);
//    }
//
//    @Override
//    public <T> List<T> toJavaList(String json, Type type) {
//        return gson.fromJson(json, TypeToken.getParameterized(List.class, type).getType());
//    }
//
//    @Override
//    public String toJson(Object obj) {
//        return gson.toJson(obj);
//    }
//}
