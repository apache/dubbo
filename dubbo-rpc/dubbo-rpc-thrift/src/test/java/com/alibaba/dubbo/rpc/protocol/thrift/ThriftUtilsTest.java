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
package com.alibaba.dubbo.rpc.protocol.thrift;

import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.rpc.gen.dubbo.$__DemoStub;

import org.junit.Assert;
import org.junit.Test;

public class ThriftUtilsTest {

    @Test
    public void testGenerateMethodArgsClassName() {

        Assert.assertEquals(
                $__DemoStub.echoString_args.class.getName(),
                ThriftUtils.generateMethodArgsClassName(
                        com.alibaba.dubbo.rpc.gen.dubbo.Demo.class.getName(),
                        "echoString"));

        Assert.assertEquals(
                $__DemoStub.echoString_args.class.getName(),
                ExtensionLoader.getExtensionLoader(ClassNameGenerator.class)
                        .getExtension(DubboClassNameGenerator.NAME).generateArgsClassName(
                        com.alibaba.dubbo.rpc.gen.dubbo.Demo.class.getName(), "echoString"));

    }

    @Test
    public void testGenerateMethodResultClassName() {

        Assert.assertEquals($__DemoStub.echoString_result.class.getName(),
                ThriftUtils.generateMethodResultClassName(
                        com.alibaba.dubbo.rpc.gen.dubbo.Demo.class.getName(),
                        "echoString"));

        Assert.assertEquals($__DemoStub.echoString_result.class.getName(),
                ExtensionLoader.getExtensionLoader(ClassNameGenerator.class)
                        .getExtension(DubboClassNameGenerator.NAME).generateResultClassName(
                        com.alibaba.dubbo.rpc.gen.dubbo.Demo.class.getName(), "echoString"));

    }

    @Test
    public void testGenerateMethodArgsClassNameThrift() {
        Assert.assertEquals(com.alibaba.dubbo.rpc.gen.thrift.Demo.echoString_args.class.getName(),
                ThriftUtils.generateMethodArgsClassNameThrift(
                        com.alibaba.dubbo.rpc.gen.thrift.Demo.Iface.class.getName(),
                        "echoString"));

        Assert.assertEquals(com.alibaba.dubbo.rpc.gen.thrift.Demo.echoString_args.class.getName(),
                ExtensionLoader.getExtensionLoader(ClassNameGenerator.class)
                        .getExtension(ThriftClassNameGenerator.NAME).generateArgsClassName(
                        com.alibaba.dubbo.rpc.gen.thrift.Demo.Iface.class.getName(),
                        "echoString"));

    }

    @Test
    public void testGenerateMethodResultClassNameThrift() {
        Assert.assertEquals(com.alibaba.dubbo.rpc.gen.thrift.Demo.echoString_result.class.getName(),
                ThriftUtils.generateMethodResultClassNameThrift(
                        com.alibaba.dubbo.rpc.gen.thrift.Demo.Iface.class.getName(),
                        "echoString"));

        Assert.assertEquals(com.alibaba.dubbo.rpc.gen.thrift.Demo.echoString_result.class.getName(),
                ExtensionLoader.getExtensionLoader(ClassNameGenerator.class)
                        .getExtension(ThriftClassNameGenerator.NAME).generateResultClassName(
                        com.alibaba.dubbo.rpc.gen.thrift.Demo.Iface.class.getName(),
                        "echoString"));

    }

}
