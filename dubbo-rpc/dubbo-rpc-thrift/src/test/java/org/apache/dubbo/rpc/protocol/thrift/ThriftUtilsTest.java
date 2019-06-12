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
package org.apache.dubbo.rpc.protocol.thrift;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.gen.dubbo.$__DemoStub;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ThriftUtilsTest {

    @Test
    public void testGenerateMethodArgsClassName() {

        Assertions.assertEquals(
                $__DemoStub.echoString_args.class.getName(),
                ThriftUtils.generateMethodArgsClassName(
                        org.apache.dubbo.rpc.gen.dubbo.Demo.class.getName(),
                        "echoString"));

        Assertions.assertEquals(
                $__DemoStub.echoString_args.class.getName(),
                ExtensionLoader.getExtensionLoader(ClassNameGenerator.class)
                        .getExtension(DubboClassNameGenerator.NAME).generateArgsClassName(
                        org.apache.dubbo.rpc.gen.dubbo.Demo.class.getName(), "echoString"));

    }

    @Test
    public void testGenerateMethodResultClassName() {

        Assertions.assertEquals($__DemoStub.echoString_result.class.getName(),
                ThriftUtils.generateMethodResultClassName(
                        org.apache.dubbo.rpc.gen.dubbo.Demo.class.getName(),
                        "echoString"));

        Assertions.assertEquals($__DemoStub.echoString_result.class.getName(),
                ExtensionLoader.getExtensionLoader(ClassNameGenerator.class)
                        .getExtension(DubboClassNameGenerator.NAME).generateResultClassName(
                        org.apache.dubbo.rpc.gen.dubbo.Demo.class.getName(), "echoString"));

    }

    @Test
    public void testGenerateMethodArgsClassNameThrift() {
        Assertions.assertEquals(org.apache.dubbo.rpc.gen.thrift.Demo.echoString_args.class.getName(),
                ThriftUtils.generateMethodArgsClassNameThrift(
                        org.apache.dubbo.rpc.gen.thrift.Demo.Iface.class.getName(),
                        "echoString"));

        Assertions.assertEquals(org.apache.dubbo.rpc.gen.thrift.Demo.echoString_args.class.getName(),
                ExtensionLoader.getExtensionLoader(ClassNameGenerator.class)
                        .getExtension(ThriftClassNameGenerator.NAME).generateArgsClassName(
                        org.apache.dubbo.rpc.gen.thrift.Demo.Iface.class.getName(),
                        "echoString"));

    }

    @Test
    public void testGenerateMethodResultClassNameThrift() {
        Assertions.assertEquals(org.apache.dubbo.rpc.gen.thrift.Demo.echoString_result.class.getName(),
                ThriftUtils.generateMethodResultClassNameThrift(
                        org.apache.dubbo.rpc.gen.thrift.Demo.Iface.class.getName(),
                        "echoString"));

        Assertions.assertEquals(org.apache.dubbo.rpc.gen.thrift.Demo.echoString_result.class.getName(),
                ExtensionLoader.getExtensionLoader(ClassNameGenerator.class)
                        .getExtension(ThriftClassNameGenerator.NAME).generateResultClassName(
                        org.apache.dubbo.rpc.gen.thrift.Demo.Iface.class.getName(),
                        "echoString"));

    }

}
