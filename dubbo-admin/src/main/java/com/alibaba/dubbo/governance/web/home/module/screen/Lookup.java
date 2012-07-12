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
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.governance.service.ConsumerService;
import com.alibaba.dubbo.registry.RegistryService;

/**
 * @author tony.chenl
 */
public class Lookup extends Restful{
    
    @Autowired
    ConsumerService consumerDAO;
    
    @Autowired
    private RegistryService registryService;

    public Result doExecute(Map<String, Object> context) throws Exception {
        String inf = request.getParameter("interface");
        if(inf==null || inf.isEmpty()){
            throw new IllegalArgumentException("please give me the interface");
        }
        String group = null;
        if(inf.contains("/")) {
            int idx = inf.indexOf('/');
            group = inf.substring(idx);
            inf = inf.substring(idx + 1, inf.length());
        }
        String version = null;
        if(inf.contains(":")) {
            int idx = inf.lastIndexOf(':');
            version = inf.substring(idx + 1, inf.length());
            inf = inf.substring(idx);
        }
        
        String parameters = request.getParameter("parameters");
        String url = "subscribe://" + operatorAddress + "/" + request.getParameter("interface") ;
        if(parameters != null && parameters.trim().length() > 0) {
            url += parameters.trim();
        }
        
        URL u = URL.valueOf(url);
        if(group != null) {
            u.addParameter("group", group);
        }
        
        if(version != null) u.addParameter("version", version);
        
        List<URL> lookup = registryService.lookup(u);
        
        Map<String, Map<String, String>> serviceUrl = new HashMap<String, Map<String,String>>();
        Map<String, String> urls = new HashMap<String, String>();
        serviceUrl.put(request.getParameter("interface").trim(), urls);
        
        for(URL u2 : lookup) {
            urls.put(u2.toIdentityString(), u2.toParameterString());
        }
        
        Result result = new Result();
        result.setMessage(serviceUrl);
        return result;
    }

}
