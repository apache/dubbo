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
package org.apache.dubbo.rpc.protocol.dubbo;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.model.ModuleServiceRepository;
import org.apache.dubbo.rpc.protocol.dubbo.support.ProtocolUtils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.apache.dubbo.common.constants.CommonConstants.CALLBACK_INSTANCES_LIMIT_KEY;

class ArgumentCallbackTest {

    protected Exporter<IDemoService> exporter = null;
    protected Exporter<IHelloService> hello_exporter = null;
    protected Invoker<IDemoService> reference = null;
    protected URL serviceURL = null;
    protected URL consumerUrl = null;
    // ============================A gorgeous line of segmentation================================================
    IDemoService demoProxy = null;

    @AfterEach
    public void tearDown() {
        destroyService();
        ProtocolUtils.closeAll();
    }

    public void exportService() {
        // export one service first, to test connection sharing
        serviceURL = serviceURL.addParameter("connections", 1);
        URL hellourl = serviceURL.setPath(IHelloService.class.getName());
        ModuleServiceRepository serviceRepository = ApplicationModel.defaultModel().getDefaultModule().getServiceRepository();
        serviceRepository.registerService(IDemoService.class);
        serviceRepository.registerService(IHelloService.class);
        hello_exporter = ProtocolUtils.export(new HelloServiceImpl(), IHelloService.class, hellourl);
        exporter = ProtocolUtils.export(new DemoServiceImpl(), IDemoService.class, serviceURL);
    }

    void referService() {
        ApplicationModel.defaultModel().getDefaultModule().getServiceRepository().registerService(IDemoService.class);
        demoProxy = (IDemoService) ProtocolUtils.refer(IDemoService.class, consumerUrl);
    }

    @BeforeEach
    public void setUp() {
    }

    public void initOrResetUrl(int callbacks, int timeout) throws Exception {
        int port = NetUtils.getAvailablePort();
        consumerUrl = serviceURL = URL.valueOf("dubbo://127.0.0.1:" + port + "/" + IDemoService.class.getName() + "?group=test"
                + "&xxx.0.callback=true"
                + "&xxx2.0.callback=true"
                + "&unxxx2.0.callback=false"
                + "&timeout=" + timeout
                + "&retries=0"
                + "&" + CALLBACK_INSTANCES_LIMIT_KEY + "=" + callbacks)
            .setScopeModel(ApplicationModel.defaultModel().getDefaultModule())
            .setServiceModel(new ConsumerModel(IDemoService.class.getName(), null, null,
                ApplicationModel.defaultModel().getDefaultModule(), null, null, ClassUtils.getClassLoader(IDemoService.class)));

        //      uncomment is unblock invoking
//        serviceURL = serviceURL.addParameter("yyy."+Constants.ASYNC_KEY,String.valueOf(true));
//        consumerUrl = consumerUrl.addParameter("yyy."+Constants.ASYNC_KEY,String.valueOf(true));
    }

    public void initOrResetService() {
        destroyService();
        exportService();
        referService();
    }

    public void destroyService() {
        ApplicationModel.defaultModel().getApplicationServiceRepository().destroy();
        demoProxy = null;
        try {
            if (exporter != null) exporter.unexport();
            if (hello_exporter != null) hello_exporter.unexport();
            if (reference != null) reference.destroy();
        } catch (Exception e) {
        }
    }

    @Test
    void TestCallbackNormalWithBindPort() throws Exception {
        initOrResetUrl(1, 10000000);
        consumerUrl = serviceURL.addParameter(Constants.BIND_PORT_KEY, "7653");
        initOrResetService();

        final AtomicInteger count = new AtomicInteger(0);

        demoProxy.xxx(new IDemoCallback() {
            public String yyy(String msg) {
                System.out.println("Recived callback: " + msg);
                count.incrementAndGet();
                return "ok";
            }
        }, "other custom args", 10, 100);
        System.out.println("Async...");
        assertCallbackCount(10, 100, count);
        destroyService();

    }

    @Test
    void TestCallbackNormal() throws Exception {

        initOrResetUrl(1, 10000000);
        initOrResetService();
        final AtomicInteger count = new AtomicInteger(0);

        demoProxy.xxx(new IDemoCallback() {
            public String yyy(String msg) {
                System.out.println("Recived callback: " + msg);
                count.incrementAndGet();
                return "ok";
            }
        }, "other custom args", 10, 100);
        System.out.println("Async...");
//        Thread.sleep(10000000);
        assertCallbackCount(10, 100, count);
        destroyService();


    }

    @Test
    void TestCallbackMultiInstans() throws Exception {
        initOrResetUrl(2, 3000);
        initOrResetService();
        IDemoCallback callback = new IDemoCallback() {
            public String yyy(String msg) {
                System.out.println("callback1:" + msg);
                return "callback1 onChanged ," + msg;
            }
        };

        IDemoCallback callback2 = new IDemoCallback() {
            public String yyy(String msg) {
                System.out.println("callback2:" + msg);
                return "callback2 onChanged ," + msg;
            }
        };
        {
            demoProxy.xxx2(callback);
            Assertions.assertEquals(1, demoProxy.getCallbackCount());
            Thread.sleep(500);
            demoProxy.unxxx2(callback);
            Assertions.assertEquals(0, demoProxy.getCallbackCount());

            demoProxy.xxx2(callback2);
            Assertions.assertEquals(1, demoProxy.getCallbackCount());
            Thread.sleep(500);
            demoProxy.unxxx2(callback2);
            Assertions.assertEquals(0, demoProxy.getCallbackCount());

            demoProxy.xxx2(callback);
            Thread.sleep(500);
            Assertions.assertEquals(1, demoProxy.getCallbackCount());
            demoProxy.unxxx2(callback);
            Assertions.assertEquals(0, demoProxy.getCallbackCount());
        }
        {
            demoProxy.xxx2(callback);
            Assertions.assertEquals(1, demoProxy.getCallbackCount());

            demoProxy.xxx2(callback);
            Assertions.assertEquals(1, demoProxy.getCallbackCount());

            demoProxy.xxx2(callback2);
            Assertions.assertEquals(2, demoProxy.getCallbackCount());
        }
        destroyService();
    }

    @Test
    void TestCallbackConsumerLimit() throws Exception {
        Assertions.assertThrows(RpcException.class, () -> {
            initOrResetUrl(1, 1000);
            // URL cannot be transferred automatically from the server side to the client side by using API, instead,
            // it needs manually specified.
            initOrResetService();
            final AtomicInteger count = new AtomicInteger(0);
            demoProxy.xxx(new IDemoCallback() {
                public String yyy(String msg) {
                    System.out.println("Recived callback: " + msg);
                    count.incrementAndGet();
                    return "ok";
                }
            }, "other custom args", 10, 100);

            demoProxy.xxx(new IDemoCallback() {
                public String yyy(String msg) {
                    System.out.println("Recived callback: " + msg);
                    count.incrementAndGet();
                    return "ok";
                }
            }, "other custom args", 10, 100);
            destroyService();
        });
    }

    @Test
    void TestCallbackProviderLimit() throws Exception {
        Assertions.assertThrows(RpcException.class, () -> {
            initOrResetUrl(1, 1000);
            // URL cannot be transferred automatically from the server side to the client side by using API, instead,
            // it needs manually specified.
            serviceURL = serviceURL.addParameter(CALLBACK_INSTANCES_LIMIT_KEY, 1 + "");
            initOrResetService();
            final AtomicInteger count = new AtomicInteger(0);
            demoProxy.xxx(new IDemoCallback() {
                public String yyy(String msg) {
                    System.out.println("Recived callback: " + msg);
                    count.incrementAndGet();
                    return "ok";
                }
            }, "other custom args", 10, 100);

            demoProxy.xxx(new IDemoCallback() {
                public String yyy(String msg) {
                    System.out.println("Recived callback: " + msg);
                    count.incrementAndGet();
                    return "ok";
                }
            }, "other custom args", 10, 100);
            destroyService();
        });
    }

    private void assertCallbackCount(int runs, int sleep, AtomicInteger count) throws InterruptedException {
        int last = count.get();
        for (int i = 0; i < runs; i++) {
            if (last > runs) break;
            Thread.sleep(sleep * 2);
            System.out.println(count.get() + "  " + last);
            Assertions.assertTrue(count.get() > last);
            last = count.get();
        }
        // has one sync callback
        Assertions.assertEquals(runs + 1, count.get());
    }

    @Disabled("need start with separate process")
    @Test
    void startProvider() throws Exception {
        exportService();
        synchronized (ArgumentCallbackTest.class) {
            ArgumentCallbackTest.class.wait();
        }
    }

    interface IDemoCallback {
        String yyy(String msg);
    }

    interface IHelloService {
        String sayHello();
    }

    interface IDemoService {
        String get();

        int getCallbackCount();

        void xxx(IDemoCallback callback, String arg1, int runs, int sleep);

        void xxx2(IDemoCallback callback);

        void unxxx2(IDemoCallback callback);
    }

    class HelloServiceImpl implements IHelloService {
        public String sayHello() {
            return "hello";
        }

    }

    class DemoServiceImpl implements IDemoService {
        private List<IDemoCallback> callbacks = new ArrayList<IDemoCallback>();
        private volatile Thread t = null;
        private volatile Lock lock = new ReentrantLock();

        public String get() {
            return "ok";
        }

        public void xxx(final IDemoCallback callback, String arg1, final int runs, final int sleep) {
            callback.yyy("Sync callback msg .This is callback data. arg1:" + arg1);
            Thread t = new Thread(new Runnable() {
                public void run() {
                    for (int i = 0; i < runs; i++) {
                        String ret = callback.yyy("server invoke callback : arg:" + System.currentTimeMillis());
                        System.out.println("callback result is :" + ret);
                        try {
                            Thread.sleep(sleep);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            t.setDaemon(true);
            t.start();
            System.out.println("xxx invoke complete");
        }

        public int getCallbackCount() {
            return callbacks.size();
        }

        public void xxx2(IDemoCallback callback) {
            if (!callbacks.contains(callback)) {
                callbacks.add(callback);
            }
            startThread();
        }

        private void startThread() {
            if (t == null || callbacks.size() == 0) {
                try {
                    lock.lock();
                    t = new Thread(new Runnable() {
                        public void run() {
                            while (callbacks.size() > 0) {
                                try {
                                    List<IDemoCallback> callbacksCopy = new ArrayList<IDemoCallback>(callbacks);
                                    for (IDemoCallback callback : callbacksCopy) {
                                        try {
                                            callback.yyy("this is callback msg,current time is :" + System.currentTimeMillis());
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            callbacks.remove(callback);
                                        }
                                    }
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                    t.setDaemon(true);
                    t.start();
                } finally {
                    lock.unlock();
                }
            }
        }

        public void unxxx2(IDemoCallback callback) {
            if (!callbacks.contains(callback)) {
                throw new IllegalStateException("callback instance not found");
            }
            callbacks.remove(callback);
        }
    }
}
