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
package org.apache.dubbo.demo.consumer;

import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.rpc.service.GenericService;

import java.util.HashMap;
import java.util.Map;

public class Application {
    /**
     * In order to make sure multicast registry works, need to specify '-Djava.net.preferIPv4Stack=true' before
     * launch the application
     */
    public static void main(String[] args) {
        ReferenceConfig<GenericService> reference = new ReferenceConfig<GenericService>();
        // 弱类型接口名
        reference.setInterface("org.apache.dubbo.demo.provider.DemoGenericService");
        reference.setVersion("1.0.0");
        // 声明为泛化接口
        reference.setGeneric(true);


        // 用org.apache.dubbo.rpc.service.GenericService可以替代所有接口引用
        GenericService genericService = reference.get();

        Map<String, String> extraMap = new HashMap<>();
        extraMap.put("a", "aa");
        extraMap.put("b", "bb");
        Map<String, Object> param = new HashMap<>();
        param.put("_name", "kobe");
        param.put("_description", "lakers");
        param.put("extra", extraMap);
        param.put("lValue","1123");

        // 基本类型以及Date,List,Map等不需要转换，直接调用
        Map<String, Object> result = (Map<String, Object>) genericService.$invoke("forGeneric", new String[]{"org.apache.dubbo.demo.provider.RequestDemo"}, new Object[]{param});
        System.out.println(result);
        if (!result.get("_extraInfo").equals(extraMap)) {
            throw new RuntimeException("receive error result");
        }
    }
}
