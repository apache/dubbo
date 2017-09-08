/*
 * Copyright 1999-2101 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.governance.web.home.module.screen;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * UnReg.java
 *
 * @author tony.chenl
 */
public class Unreg extends Restful {

    public Result doExecute(Map<String, Object> context) throws Exception {
        if (url == null) {
            throw new IllegalArgumentException("please give me the url");
        }
        if (url.getPath().isEmpty()) {
            throw new IllegalArgumentException("please use interface as your url path");
        }
        HashMap<String, Set<String>> services = new HashMap<String, Set<String>>();
        Set<String> serviceUrl = new HashSet<String>();
        serviceUrl.add(url.toIdentityString());
        String name = url.getPath();
        String version = url.getParameter("version");
        if (version != null) {
            name = name + ":" + version;
        }
        String group = url.getParameter("group");
        if (group != null) {
            name = group + "/" + name;
        }
        services.put(name, serviceUrl);
//        registryService.unregister(operatorAddress,services);
        Result result = new Result();
        result.setMessage("Unregister Successfully!");
        return result;
    }

}
