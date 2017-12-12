/**
 * File Created at 2011-12-08
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

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.gen.dubbo.Demo;

import org.junit.After;
import org.junit.Before;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class ThriftProtocolTest extends AbstractTest {

    public static final int DEFAULT_PORT = 30660;

    private ThriftProtocol protocol;

    private Invoker<Demo> invoker;

    private URL url;

    @Before
    public void setUp() throws Exception {

        init();

        protocol = new ThriftProtocol();

        url = URL.valueOf(ThriftProtocol.NAME + "://127.0.0.1:" + PORT + "/" + Demo.class.getName());

    }

    @After
    public void tearDown() throws Exception {

        destroy();

        if (protocol != null) {
            protocol.destroy();
            protocol = null;
        }

        if (invoker != null) {
            invoker.destroy();
            invoker = null;
        }

    }
/*
    @Test
    public void testRefer() throws Exception {
        // FIXME
        *//*invoker = protocol.refer( Demo.class, url );

        Assert.assertNotNull( invoker );

        RpcInvocation invocation = new RpcInvocation();

        invocation.setMethodName( "echoString" );

        invocation.setParameterTypes( new Class<?>[]{ String.class } );

        String arg = "Hello, World!";

        invocation.setArguments( new Object[] { arg } );

        Result result = invoker.invoke( invocation );

        Assert.assertEquals( arg, result.getResult() );*//*

    }*/

}
