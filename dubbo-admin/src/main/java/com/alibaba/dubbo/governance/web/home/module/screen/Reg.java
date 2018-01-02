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
package com.alibaba.dubbo.governance.web.home.module.screen;

import java.util.HashMap;
import java.util.Map;

public class Reg extends Restful {

    public Result doExecute(Map<String, Object> context) throws Exception {
        if (url == null) {
            throw new IllegalArgumentException("please give me the url");
        }
        if (url.getPath().isEmpty()) {
            throw new IllegalArgumentException("please use interface as your url path");
        }
        Map<String, String> tmp = new HashMap<String, String>();
        tmp.put(url.toIdentityString(), url.toParameterString());
        Map<String, Map<String, String>> register = new HashMap<String, Map<String, String>>();
        register.put(url.getPath(), tmp);
//        Map<String, Map<String, String>> newRegister = RegistryUtils.convertRegister(register);
//        registryService.register(operatorAddress, newRegister, false);
        Result result = new Result();
        result.setMessage("Register Successfully!");
        return result;
    }

}
