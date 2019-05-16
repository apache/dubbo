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
package org.apache.dubbo.rpc.protocol.nativethrift;


import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.protocol.AbstractProxyProtocol;

import org.apache.thrift.TException;
import org.apache.thrift.TMultiplexedProcessor;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadedSelectorServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import java.lang.reflect.Constructor;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 *  native thrift protocol
 */
public class ThriftProtocol extends AbstractProxyProtocol {

    public static final int DEFAULT_PORT = 40880;

    public static final String NAME = "native-thrift";
    public static final String THRIFT_IFACE = "$Iface";
    public static final String THRIFT_PROCESSOR = "$Processor";
    public static final String THRIFT_CLIENT = "$Client";

    private static final Map<String, TServer> serverMap = new HashMap<>();
    private TMultiplexedProcessor processor = new TMultiplexedProcessor();

    @Override
    public int getDefaultPort() {
        return DEFAULT_PORT;
    }

    public ThriftProtocol() {
        super(TException.class, RpcException.class);
    }

    @Override
    protected <T> Runnable doExport(T impl, Class<T> type, URL url) throws RpcException {
        return exportThreadedSelectorServer(impl, type, url);
    }

    @Override
    protected <T> T doRefer(Class<T> type, URL url) throws RpcException {
        return doReferFrameAndCompact(type, url);
    }

    public ThriftProtocol(Class<?>... exceptions) {
        super(exceptions);
    }

    private <T> Runnable exportThreadedSelectorServer(T impl, Class<T> type, URL url) throws RpcException {

        TThreadedSelectorServer.Args tArgs = null;
        String typeName = type.getName();

        TServer tserver = null;
        if (typeName.endsWith(THRIFT_IFACE)) {
            String processorClsName = typeName.substring(0, typeName.indexOf(THRIFT_IFACE)) + THRIFT_PROCESSOR;
            try {
                Class<?> clazz = Class.forName(processorClsName);
                Constructor constructor = clazz.getConstructor(type);
                try {
                    TProcessor tprocessor = (TProcessor) constructor.newInstance(impl);
                    processor.registerProcessor(typeName,tprocessor);

                    tserver = serverMap.get(url.getAddress());
                    if(tserver == null) {

                        /**Solve the problem of only 50 of the default number of concurrent connections*/
                        TNonblockingServerSocket.NonblockingAbstractServerSocketArgs args = new TNonblockingServerSocket.NonblockingAbstractServerSocketArgs();
                        /**1000 connections*/
                        args.backlog(1000);
                        args.bindAddr(new InetSocketAddress(url.getHost(), url.getPort()));
                        /**timeout: 10s */
                        args.clientTimeout(10000);

                        TNonblockingServerSocket transport = new TNonblockingServerSocket(args);

                        tArgs = new TThreadedSelectorServer.Args(transport);
                        tArgs.workerThreads(200);
                        tArgs.selectorThreads(4);
                        tArgs.acceptQueueSizePerThread(256);
                        tArgs.processor(processor);
                        tArgs.transportFactory(new TFramedTransport.Factory());
                        tArgs.protocolFactory(new TCompactProtocol.Factory());
                    }else{
                        return null; // if server is starting, return and do nothing here
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                    throw new RpcException("Fail to create nativethrift server(" + url + ") : " + e.getMessage(), e);
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new RpcException("Fail to create nativethrift server(" + url + ") : " + e.getMessage(), e);
            }
        }

        if (tserver == null && tArgs == null) {
            logger.error("Fail to create nativethrift server(" + url + ") due to null args");
            throw new RpcException("Fail to create nativethrift server(" + url + ") due to null args");
        }
        final TServer thriftServer =  new TThreadedSelectorServer(tArgs);
        serverMap.put(url.getAddress(),thriftServer);

        new Thread(() -> {
            logger.info("Start Thrift ThreadedSelectorServer");
            thriftServer.serve();
            logger.info("Thrift ThreadedSelectorServer started.");
        }).start();

        return () -> {
            try {
                logger.info("Close Thrift NonblockingServer");
                thriftServer.stop();
            } catch (Throwable e) {
                logger.warn(e.getMessage(), e);
            }
        };
    }

    private <T> T doReferFrameAndCompact(Class<T> type, URL url) throws RpcException {

        try {
            T thriftClient = null;
            String typeName = type.getName();
            if (typeName.endsWith(THRIFT_IFACE)) {
                String clientClsName = typeName.substring(0, typeName.indexOf(THRIFT_IFACE)) + THRIFT_CLIENT;
                Class<?> clazz = Class.forName(clientClsName);
                Constructor constructor = clazz.getConstructor(TProtocol.class);
                try {
                    TSocket tSocket = new TSocket(url.getHost(), url.getPort());
                    TTransport transport = new TFramedTransport(tSocket);
                    TProtocol tprotocol = new TCompactProtocol(transport);
                    TMultiplexedProtocol protocol = new TMultiplexedProtocol(tprotocol,typeName);
                    thriftClient = (T) constructor.newInstance(protocol);
                    transport.open();
                    logger.info("nativethrift client opened for service(" + url + ")");
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                    throw new RpcException("Fail to create remote client:" + e.getMessage(), e);
                }
            }
            return thriftClient;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new RpcException("Fail to create remote client for service(" + url + "): " + e.getMessage(), e);
        }
    }

}
