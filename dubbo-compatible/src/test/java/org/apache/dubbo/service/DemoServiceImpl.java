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
package org.apache.dubbo.service;

import org.apache.dubbo.rpc.RpcContext;

import java.util.List;

/**
 * DemoServiceImpl
 */

public class DemoServiceImpl implements DemoService {
    public DemoServiceImpl() {
        super();
    }

    public String sayHello(String name) {
        return "hello " + name;
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

    public Type enumlength(Type type) {
        return type;
    }

    public int stringLength(String str) {
        return str.length();
    }

    public String get(CustomArgument arg1) {
        return arg1.toString();
    }

    public byte getbyte(byte arg) {
        return arg;
    }

    @Override
    public String complexCompute(String input, ComplexObject co) {
        return input + "###" + co.toString();
    }

    @Override
    public ComplexObject findComplexObject(String var1, int var2, long l, String[] var3, List<Integer> var4, ComplexObject.TestEnum testEnum) {

        return new ComplexObject(var1, var2, l, var3, var4, testEnum);
    }

    public Person gerPerson(Person person) {
        return person;
    }
}
