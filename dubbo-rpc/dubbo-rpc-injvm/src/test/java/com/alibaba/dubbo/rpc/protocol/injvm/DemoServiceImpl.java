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
package com.alibaba.dubbo.rpc.protocol.injvm;

import com.alibaba.dubbo.rpc.RpcContext;

/**
 * DemoServiceImpl
 */

public class DemoServiceImpl implements DemoService {
    public DemoServiceImpl() {
        super();
    }

    public void sayHello(String name) {
        System.out.println("hello " + name);
    }

    public String echo(String text) {
        return text;
    }

    public long timestamp() {
        return System.currentTimeMillis();
    }

    public String getThreadName() {
        return Thread.currentThread().getName();
    }

    public int getSize(String[] strs) {
        if (strs == null)
            return -1;
        return strs.length;
    }

    public int getSize(Object[] os) {
        if (os == null)
            return -1;
        return os.length;
    }

    public Object invoke(String service, String method) throws Exception {
        System.out.println("RpcContext.getContext().getRemoteHost()=" + RpcContext.getContext().getRemoteHost());
        return service + ":" + method;
    }

    public Type enumlength(Type... types) {
        if (types.length == 0)
            return Type.Lower;
        return types[0];
    }

    public int stringLength(String str) {
        return str.length();
    }
}