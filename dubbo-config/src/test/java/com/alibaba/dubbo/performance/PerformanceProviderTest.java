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
package com.alibaba.dubbo.performance;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.junit.Test;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ProtocolConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.ServiceConfig;
import com.alibaba.dubbo.demo.api.DemoService;
import com.alibaba.dubbo.demo.provider.impl.DemoServiceImpl;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.exchange.ExchangeServer;
import com.alibaba.dubbo.remoting.exchange.Exchangers;
import com.alibaba.dubbo.remoting.exchange.support.ExchangeHandlerAdapter;
import com.alibaba.dubbo.rpc.Protocol;

/**
 * PerformanceServer
 * 
 * mvn clean test -Dtest=*PerformanceProviderTest -Dport=9911
 * 
 * @author william.liangf
 */
public class PerformanceProviderTest extends TestCase {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceProviderTest.class);
    
    private static ServiceConfig<DemoService> service = null;
    
    @Test
    public void testServer() throws Exception {
        // 读取参数
        if (PerformanceUtils.getProperty("port", null) == null) {
            logger.warn("Please set -Dport=9911");
            return;
        }
        final int port = PerformanceUtils.getIntProperty("port", 9911);
        final boolean telnet = PerformanceUtils.getBooleanProperty("telnet", true);
        if(telnet) {
            statTelnetServer(port+1);
        }
        service = statServer();
        synchronized (PerformanceProviderTest.class) {
            while (true) {
                try {
                    PerformanceProviderTest.class.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }
    
    private static void restartServer(int times, int wait, int sleep) throws Exception{
        if (service != null) {
            service.unexport();
            ExtensionLoader.getExtensionLoader(Protocol.class).getExtension("dubbo").destroy();
            Thread.sleep(100);
        }
        for (int i = 0; i < times; i++) {
            logger.info("restart times:" + i);
            service = statServer();
            if (wait > 0) {
                Thread.sleep(wait);
            }
            service.unexport();
            ExtensionLoader.getExtensionLoader(Protocol.class).getExtension("dubbo").destroy();
            if (sleep > 0) {
                Thread.sleep(sleep);
            }
        }
        service = statServer();
    }
    
    @SuppressWarnings("deprecation")
    private static ServiceConfig<DemoService> statServer() throws Exception{
        final int port = PerformanceUtils.getIntProperty("port", 9911);
        final String transporter = PerformanceUtils.getProperty(Constants.TRANSPORTER_KEY, Constants.DEFAULT_TRANSPORTER);
        final String serialization = PerformanceUtils.getProperty(Constants.SERIALIZATION_KEY, Constants.DEFAULT_REMOTING_SERIALIZATION);
        final String threadpool = PerformanceUtils.getProperty(Constants.THREADPOOL_KEY, Constants.DEFAULT_THREADPOOL);
        final int threads = PerformanceUtils.getIntProperty(Constants.THREADS_KEY, Constants.DEFAULT_THREADS);
        final int iothreads = PerformanceUtils.getIntProperty(Constants.IO_THREADS_KEY, Constants.DEFAULT_IO_THREADS);
        final int buffer = PerformanceUtils.getIntProperty(Constants.BUFFER_KEY, Constants.DEFAULT_BUFFER_SIZE);
        
        List<String> scene = new ArrayList<String>();
        scene.add("Transporter: " + transporter);
        scene.add("Service Threads: " + threads);
        PerformanceUtils.setScene(scene);
        
        DemoService demoService = new DemoServiceImpl();
        
        ApplicationConfig application = new ApplicationConfig();
        application.setName("performance-provider");
        
        RegistryConfig registry = new RegistryConfig();
        registry.setAddress(RegistryConfig.NO_AVAILABLE);
        
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setName("dubbo");
        protocol.setPort(port);
        protocol.setTransporter(transporter);
        protocol.setSerialization(serialization);
        protocol.setThreadpool(threadpool);
        protocol.setThreads(threads);
        protocol.setIothreads(iothreads);
        protocol.setBuffer(buffer);
        
        ServiceConfig<DemoService> service = new ServiceConfig<DemoService>();
        service.setApplication(application);
        service.setRegistry(registry);
        service.setProtocol(protocol);
        service.setInterfaceClass(DemoService.class);
        service.setVersion("1.0.0");
        service.setRef(demoService);
        service.export();
        
        return service;
    }
    
    private static ExchangeServer statTelnetServer(int port) throws Exception{
       // 启动服务器
        ExchangeServer telnetserver = Exchangers.bind("exchange://0.0.0.0:" + port , new ExchangeHandlerAdapter() {
            public String telnet(Channel channel, String message) throws RemotingException {
                if(message.equals("help")){
                    return "support cmd: \r\n\tstart \r\n\tstop \r\n\tshutdown \r\n\trestart times [alive] [sleep] \r\ntelnet>";
                } else if(message.equals("stop")){
                    if (service != null) {
                        logger.info("server closed:" + service);
                        service.unexport();
                        ExtensionLoader.getExtensionLoader(Protocol.class).getExtension("dubbo").destroy();
                    }
                    return "stop server\r\ntelnet>";
                } else if(message.startsWith("start")){
                    try {
                        restartServer(0, 0, 0);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return "start server\r\ntelnet>";
                } else if(message.startsWith("shutdown")){
                        System.exit(0);
                    return "start server\r\ntelnet>";
                } else if(message.startsWith("restart ")){ //r times [sleep] r 10 or r 10 100
                    String[] args = message.split(" ");
                    int times = args.length > 1 ? Integer.parseInt(args[1]) : 1;
                    int alive = args.length > 2 ? Integer.parseInt(args[2]) : 0;
                    int sleep = args.length > 3 ? Integer.parseInt(args[3]) : 100;
                    try {
                        restartServer(times, alive, sleep);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return "restart server,times:"+times+" stop wait time: "+alive+",sleep time: " + sleep+" usage:restart [times] [wait] [sleep] \r\ntelnet>";
                } else{
                    return "echo: " + message + "\r\ntelnet> ";
                }
                
            }
        });
        return telnetserver;
    }

}