/**
 * File Created at 2011-12-05
 * $Id$
 * <p>
 * Copyright 2008 Alibaba.com Croporation Limited.
 * All rights reserved.
 * <p>
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.rpc.protocol.thrift;

import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.rpc.gen.dubbo.$__DemoStub;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">gang.lvg</a>
 */
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
