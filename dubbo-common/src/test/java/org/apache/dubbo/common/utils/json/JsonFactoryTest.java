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
package org.apache.dubbo.common.utils.json;

import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.json.JSON;
import org.apache.dubbo.common.json.factory.JsonFactory;
import org.apache.dubbo.common.json.impl.FastJson2Impl;
import org.apache.dubbo.common.json.impl.FastJsonImpl;
import org.apache.dubbo.common.json.impl.GsonImpl;
import org.apache.dubbo.common.json.impl.JacksonImpl;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JsonFactoryTest {

    @Test
    public void factoryTest() {

        JsonFactory adaptiveExtension = ApplicationModel.defaultModel().getAdaptiveExtension(JsonFactory.class);

        URLBuilder urlBuilder = new URLBuilder();
        JSON json = adaptiveExtension.createJson(urlBuilder);

        Assertions.assertInstanceOf(FastJsonImpl.class, json);

        urlBuilder.addParameter("json", "fastjson2");
        json = adaptiveExtension.createJson(urlBuilder);
        Assertions.assertInstanceOf(FastJson2Impl.class, json);

        urlBuilder.addParameter("json", "jackson");
        json = adaptiveExtension.createJson(urlBuilder);
        Assertions.assertInstanceOf(JacksonImpl.class, json);

        urlBuilder.addParameter("json", "gson");
        json = adaptiveExtension.createJson(urlBuilder);
        Assertions.assertInstanceOf(GsonImpl.class, json);

    }

}
