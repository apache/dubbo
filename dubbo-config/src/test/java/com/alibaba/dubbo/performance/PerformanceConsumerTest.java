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

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.TestCase;

import org.junit.Test;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.demo.api.DemoService;
import com.alibaba.dubbo.demo.api.FullAddress;
import com.alibaba.dubbo.demo.api.Person;
import com.alibaba.dubbo.demo.api.PersonInfo;
import com.alibaba.dubbo.demo.api.PersonStatus;
import com.alibaba.dubbo.demo.api.Phone;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.exchange.support.ExchangeHandlerAdapter;

/**
 * PerformanceClientTest
 * 
 * mvn clean test -Dtest=*PerformanceConsumerTest -Dserver=10.20.153.187:9911
 * 
 * @author william.liangf
 */
public class PerformanceConsumerTest extends TestCase {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceConsumerTest.class);

    @SuppressWarnings("deprecation")
    @Test
    public void testClient() throws Throwable {
        // 读取参数
        if (PerformanceUtils.getProperty("server", null) == null) {
            logger.warn("Please set -Dserver=127.0.0.1:9911");
            return;
        }
        final String server = System.getProperty("server", "127.0.0.1:9911");
        final String transporter = PerformanceUtils.getProperty(Constants.TRANSPORTER_KEY, Constants.DEFAULT_TRANSPORTER);
        final int timeout = PerformanceUtils.getIntProperty(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
        final int length = PerformanceUtils.getIntProperty("length", 1024);
        final int connections = PerformanceUtils.getIntProperty(Constants.CONNECTIONS_KEY, 1);
        final int concurrent = PerformanceUtils.getIntProperty("concurrent", 100);
        int r = PerformanceUtils.getIntProperty("runs", 1000);
        final int runs = r > 0 ? r : Integer.MAX_VALUE;
        final String onerror = PerformanceUtils.getProperty("onerror", "continue");
        
        ApplicationConfig application = new ApplicationConfig();
        application.setName("code-consumer");
        
        RegistryConfig registry = new RegistryConfig();
        registry.setAddress(RegistryConfig.NO_AVAILABLE);
        
        ReferenceConfig<DemoService> reference = new ReferenceConfig<DemoService>();
        reference.setApplication(application);
        reference.setRegistry(registry);
        reference.setInterfaceClass(DemoService.class);
        reference.setVersion("1.0.0");
        reference.setConnections(connections);
        reference.setTimeout(timeout);
        reference.setClient(transporter);
        reference.setUrl("dubbo://" + server + "/" + DemoService.class.getName());
        final DemoService demoService = reference.get();
        
        List<String> serverEnvironment = demoService.getEnvironment();
        List<String> serverScene = demoService.getScene();
        
        // 制造数据
        StringBuilder buf = new StringBuilder(length);
        for (int i = 0; i < length; i ++) {
            buf.append("A");
        }
        final String data = buf.toString();
        
        Person person = new Person();
        person.setPersonId("superman111");
        person.setLoginName("superman");
        person.setEmail("sm@1.com");
        person.setPenName("pname");
        person.setStatus(PersonStatus.ENABLED);

        ArrayList<Phone> phones = new ArrayList<Phone>();
        Phone phone1 = new Phone("86", "0571", "87654321", "001");
        Phone phone2 = new Phone("86", "0571", "87654322", "002");
        phones.add(phone1);
        phones.add(phone2);
        PersonInfo pi = new PersonInfo();
        pi.setPhones(phones);
        Phone fax = new Phone("86", "0571", "87654321", null);
        pi.setFax(fax);
        FullAddress addr = new FullAddress("CN", "zj", "3480", "wensanlu", "315000");
        pi.setFullAddress(addr);
        pi.setMobileNo("13584652131");
        pi.setMale(true);
        pi.setDepartment("b2b");
        pi.setHomepageUrl("www.capcom.com");
        pi.setJobTitle("qa");
        pi.setName("superman");
        person.setInfoProfile(pi);
        
        // 计数器
        final AtomicLong count = new AtomicLong();
        final AtomicLong error = new AtomicLong();
        final AtomicLong time = new AtomicLong();
        final AtomicLong all = new AtomicLong();
        
        // 并发调用
        final CountDownLatch latch = new CountDownLatch(concurrent);
        for (int i = 0; i < concurrent; i ++) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        long init = System.currentTimeMillis();
                        for (int i = 0; i < runs; i++) {
                            try {
                                count.incrementAndGet();
                                long start = System.currentTimeMillis();
                                String result = demoService.sayName(data);
                                long end = System.currentTimeMillis();
                                if (! result.endsWith(data)) {
                                    throw new IllegalStateException("Invalid result " + result);
                                }
                                time.addAndGet(end - start);
                            } catch (Exception e) {
                                error.incrementAndGet();
                                e.printStackTrace();
                                if ("exit".equals(onerror)) {
                                    System.exit(-1);
                                } else if ("break".equals(onerror)) {
                                    break;
                                } else if ("sleep".equals(onerror)) {
                                    try {
                                        Thread.sleep(30000);
                                    } catch (InterruptedException e1) {
                                    }
                                }
                            }
                        }
                        all.addAndGet(System.currentTimeMillis() - init);
                    } finally {
                        latch.countDown();
                    }
                }
            }).start();
        }
        
        // 输出，tps不精确，但大概反映情况
        new Thread(new Runnable() {
            public void run() {
                try{
                    SimpleDateFormat dateFormat = new SimpleDateFormat ("HH:mm:ss");
                    long lastCount = count.get();
                    long sleepTime = 2000;
                    long elapsd = sleepTime/1000;
                    boolean bfirst = true;
                    while (latch.getCount() > 0) {
                        long c = count.get()-lastCount ;
                        if(! bfirst)//第一次不准
                            System.out.println("["+dateFormat.format(new Date()) +"] count: " + count.get() + ", error: " + error.get() + ",tps:"+(c/elapsd));
                        
                        bfirst = false;
                        lastCount = count.get();
                        Thread.sleep(sleepTime);
                    } 
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
        
        latch.await();
        
        long total = count.get();
        long failed = error.get();
        long succeeded = total - failed;
        long elapsed = time.get();
        long allElapsed = all.get();
        long clientElapsed = allElapsed - elapsed;
        long art = 0;
        long qps = 0;
        long throughput = 0;
        if (elapsed > 0) {
            art = elapsed / succeeded;
            qps = concurrent * succeeded * 1000 / elapsed;
            throughput = concurrent * succeeded * length * 2 * 1000 / elapsed;
        }
        
        PerformanceUtils.printBorder();
        PerformanceUtils.printHeader("Dubbo Remoting Performance Test Report");
        PerformanceUtils.printBorder();
        PerformanceUtils.printHeader("Test Environment");
        PerformanceUtils.printSeparator();
        for (String item: serverEnvironment) {
            PerformanceUtils.printBody("Server " + item);
        }
        PerformanceUtils.printSeparator();
        List<String> clientEnvironment = PerformanceUtils.getEnvironment();
        for (String item: clientEnvironment) {
            PerformanceUtils.printBody("Client " + item);
        }
        PerformanceUtils.printSeparator();
        PerformanceUtils.printHeader("Test Scene");
        PerformanceUtils.printSeparator();
        for (String item: serverScene) {
            PerformanceUtils.printBody("Server " + item);
        }
        PerformanceUtils.printBody("Client Transporter: " + transporter);
        PerformanceUtils.printBody("Response Timeout: " + timeout + " ms");
        PerformanceUtils.printBody("Data Length: " + length + " bytes");
        PerformanceUtils.printBody("Client Shared Connections: " + connections);
        PerformanceUtils.printBody("Client Concurrent Threads: " + concurrent);
        PerformanceUtils.printBody("Run Times Per Thread: " + runs);
        PerformanceUtils.printSeparator();
        PerformanceUtils.printHeader("Test Result");
        PerformanceUtils.printSeparator();
        PerformanceUtils.printBody("Succeeded Requests: " + DecimalFormat.getIntegerInstance().format(succeeded));
        PerformanceUtils.printBody("Failed Requests: " + failed);
        PerformanceUtils.printBody("Client Elapsed Time: " + clientElapsed + " ms");
        PerformanceUtils.printBody("Average Response Time: " + art + " ms");
        PerformanceUtils.printBody("Requests Per Second: " + qps + "/s");
        PerformanceUtils.printBody("Throughput Per Second: " + DecimalFormat.getIntegerInstance().format(throughput) + " bytes/s");
        PerformanceUtils.printBorder();
    }
    
    static class PeformanceTestHandler extends ExchangeHandlerAdapter{

        public void connected(Channel channel) throws RemotingException {
            System.out.println("connected event,chanel;"+channel);
        }

        public void disconnected(Channel channel) throws RemotingException {
            System.out.println("disconnected event,chanel;"+channel);
        }
    }
}