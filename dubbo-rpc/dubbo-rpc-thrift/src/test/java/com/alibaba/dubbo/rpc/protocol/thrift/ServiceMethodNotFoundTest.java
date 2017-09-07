/**
 * File Created at 2011-12-09
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
import com.alibaba.dubbo.rpc.gen.dubbo.$__DemoStub;
import com.alibaba.dubbo.rpc.gen.dubbo.Demo;
import com.alibaba.dubbo.rpc.protocol.thrift.ext.MultiServiceProcessor;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class ServiceMethodNotFoundTest extends AbstractTest {

    private URL url;

    protected void init() throws Exception {

        TServerTransport serverTransport = new TServerSocket(PORT);

        DubboDemoImpl impl = new DubboDemoImpl();

        $__DemoStub.Processor processor = new $__DemoStub.Processor(impl);

        // for test
        Field field = processor.getClass().getSuperclass().getDeclaredField("processMap");

        field.setAccessible(true);

        Object obj = field.get(processor);

        if (obj instanceof Map) {
            ((Map) obj).remove("echoString");
        }
        // ~

        TBinaryProtocol.Factory bFactory = new TBinaryProtocol.Factory();

        MultiServiceProcessor wrapper = new MultiServiceProcessor();
        wrapper.addProcessor(Demo.class, processor);

        server = new TThreadPoolServer(
                new TThreadPoolServer.Args(serverTransport)
                        .inputProtocolFactory(bFactory)
                        .outputProtocolFactory(bFactory)
                        .inputTransportFactory(getTransportFactory())
                        .outputTransportFactory(getTransportFactory())
                        .processor(wrapper));

        Thread startTread = new Thread() {

            @Override
            public void run() {

                server.serve();
            }

        };

        startTread.start();

        while (!server.isServing()) {
            Thread.sleep(100);
        }

    }

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

    @Test
    public void testServiceMethodNotFound() throws Exception {
        // FIXME
        /*url = url.addParameter( "echoString." + Constants.TIMEOUT_KEY, Integer.MAX_VALUE );

        invoker = protocol.refer( Demo.class, url );

        org.junit.Assert.assertNotNull( invoker );

        RpcInvocation invocation = new RpcInvocation();

        invocation.setMethodName( "echoString" );

        invocation.setParameterTypes( new Class<?>[]{ String.class } );

        String arg = "Hello, World!";

        invocation.setArguments( new Object[] { arg } );
        
        invocation.setAttachment(Constants.INTERFACE_KEY, DemoImpl.class.getName());

        Result result = invoker.invoke( invocation );

        Assert.assertNull( result.getResult() );

        Assert.assertTrue( result.getException() instanceof RpcException );*/

    }

}
