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
package com.alibaba.dubbo.demo.consumer;

import com.alibaba.dubbo.demo.DemoService;
import com.alibaba.dubbo.demo.ParamCallback;
import com.alibaba.dubbo.demo.TestException;
import com.alibaba.dubbo.demo.entity.User;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.fastjson.JSON;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

public class DemoServiceImpl implements DemoService {

    public String sayHello(String name) {
        System.out.println("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] Hello " + name + ", request from consumer: " + RpcContext.getContext().getRemoteAddress());
        return "localhost: Hello " + name + ", response form provider: " + RpcContext.getContext().getLocalAddress();
    }

    @Override
    public void bye(Object o) {
        System.out.println(JSON.toJSONString(o));
        System.out.println(o.getClass());
    }

    @Override
    public void callbackParam(String msg, ParamCallback callback) {

    }

    @Override
    public String say01(String msg) {
        if ("RuntimeException".equalsIgnoreCase(msg)) {
            throw new RuntimeException("123");
        }
        if ("TestException".equalsIgnoreCase(msg)) {
            throw new TestException();
        }
        return null;
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

    @Override
    public void save(User user) {
        System.out.println("save");
    }

    @Override
    public void update(User user) {
        System.out.println("update");
    }

    @Override
    public void delete(User user, Boolean vip) {
        System.out.println("delete");
    }

    @Override
    public void saves(Collection<User> users) {
        System.out.println("saves");
    }

    @Override
    public void saves(User[] users) {
        System.out.println("saves[]");
    }

    @Override
    public void demo(String name, String password, User users) {

    }

//    public String getTest01() {
//        return test01;
//    }
//
//    public DemoServiceImpl setTest01(String test01) {
//        this.test01 = test01;
//        return this;
//    }

    //    public void setTest01(String test01) {
//        this.test01 = test01;
//    }

}