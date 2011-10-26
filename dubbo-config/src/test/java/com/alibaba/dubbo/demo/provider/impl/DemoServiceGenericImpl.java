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
package com.alibaba.dubbo.demo.provider.impl;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.service.GenericException;
import com.alibaba.dubbo.service.GenericService;

/**
 * DemoServiceGenericeImpl
 * 
 * @author william.liangf
 */
public class DemoServiceGenericImpl implements GenericService {

    public Object $invoke(String method, String[] parameterTypes, Object[] args) throws GenericException {
        if ("sayHello".equals(method)) {
            System.out.println(">>>>>>DemoServiceGenericImpl: " + args[0]);
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("salutatory", "generic provider: " + RpcContext.getContext().getLocalAddress());
            map.put("user", args[0]);
            return map;
        }
        return null;
    }

}