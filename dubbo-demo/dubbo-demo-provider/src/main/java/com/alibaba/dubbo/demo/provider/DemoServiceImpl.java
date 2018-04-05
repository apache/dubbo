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
package com.alibaba.dubbo.demo.provider;

import com.alibaba.dubbo.demo.Cat;
import com.alibaba.dubbo.demo.DemoService;
import com.alibaba.dubbo.demo.ParamCallback;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.fastjson.JSON;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DemoServiceImpl implements DemoService {

    /**
     * 测试属性，{@link com.alibaba.dubbo.common.bytecode.Wrapper}
     */
    public String test01;

    private DemoDAO demoDAO;

    public String sayHello(String name) {
        System.out.println("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] Hello " + name + ", request from consumer: " + RpcContext.getContext().getRemoteAddress());
        return "Hello " + name + ", response form provider: " + RpcContext.getContext().getLocalAddress();
    }

    @Override
    public void bye(Object o) {
        System.out.println(JSON.toJSONString(o));
        System.out.println(o.getClass());
    }

    @Override
    public void callbackParam(String msg, ParamCallback callback) {
        callback.doSome(new Cat().setName("miao"));
    }

    @Override
    public String say01(String msg) {
        return msg;
    }

    @Override
    public String[] say02() {
        return new String[0];
    }

    @Override
    public void say03() {

    }

    @Override
    public Void say04() {
        return null;
    }

    public void setDemoDAO(DemoDAO demoDAO) {
        this.demoDAO = demoDAO;
    }

}