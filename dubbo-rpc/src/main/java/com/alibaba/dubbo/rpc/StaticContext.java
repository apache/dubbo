/*
 * Copyright 1999-2011 Alibaba Group.
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
package com.alibaba.dubbo.rpc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.StringUtils;

/**
 * 系统存储，内部类.
 */
public class StaticContext extends ConcurrentHashMap<Object, Object>{
    private static final long serialVersionUID = 1L;
    private static final String SYSTEMNAME = "system";
    private String name ;
	
	private static final ConcurrentMap<String, StaticContext> context_map = new ConcurrentHashMap<String, StaticContext>() ;
	
    private StaticContext(String name) {
        super();
        this.name = name;
    }

    public String getName() {
        return name;
    }
    
    public static StaticContext getSystemContext() {
        return getContext(SYSTEMNAME);
    }

    public static StaticContext getContext(String name) {
	    StaticContext appContext = context_map.get(name);
	    if (appContext == null){
	        appContext = context_map.putIfAbsent(name, new StaticContext(name));
	        if (appContext == null){
	            appContext = context_map.get(name);
	        }
	    }
	    return appContext;
	}
    public static StaticContext remove(String name){
        return context_map.remove(name);
    }
    
    public static String getKey(URL url, String methodName, String suffix) {
        return getKey(url.getServiceKey(), methodName, suffix);
    }
    public static String getKey(Map<String, String> paras, String methodName, String suffix) {
        return getKey(StringUtils.getServiceKey(paras), methodName, suffix);
    }
    private static String getKey(String servicekey, String methodName, String suffix) {
        StringBuffer sb = new StringBuffer().append(servicekey).append(".").append(methodName).append(".").append(suffix);
        return sb.toString();
    }
}