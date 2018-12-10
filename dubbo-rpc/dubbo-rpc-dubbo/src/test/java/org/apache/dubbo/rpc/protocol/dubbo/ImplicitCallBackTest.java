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


import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ConsumerMethodModel;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.protocol.dubbo.support.ProtocolUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ImplicitCallBackTest {

    protected Exporter<IDemoService> exporter = null;
    protected Invoker<IDemoService> reference = null;

    protected URL serviceURL = null;
    protected URL consumerUrl = null;
    Method onReturnMethod;
    Method onThrowMethod;
    Method onInvokeMethod;
    NofifyImpl notify = new NofifyImpl();
    //================================================================================================
    IDemoService demoProxy = null;

    @Before
    public void setUp() throws SecurityException, NoSuchMethodException {
        onReturnMethod = Nofify.class.getMethod("onreturn", new Class<?>[]{Person.class, Integer.class});
        onThrowMethod = Nofify.class.getMethod("onthrow", new Class<?>[]{Throwable.class, Integer.class});
        onInvokeMethod = Nofify.class.getMethod("oninvoke", new Class<?>[]{Integer.class});
    }

    @After
    public void tearDown() {
        ProtocolUtils.closeAll();
    }

    public void initOrResetService() {
        destroyService();
        exportService();
        referService();
    }

    public void initOrResetExService() {
        destroyService();
        exportExService();
        referService();
    }

    public void destroyService() {
        demoProxy = null;
        try {
            if (exporter != null) exporter.unexport();
            if (reference != null) reference.destroy();
        } catch (Exception e) {
        }
    }

    void referService() {
        demoProxy = (IDemoService) ProtocolUtils.refer(IDemoService.class, consumerUrl);
    }

    public void exportService() {
        exporter = ProtocolUtils.export(new NormalDemoService(), IDemoService.class, serviceURL);
    }

    public void exportExService() {
        exporter = ProtocolUtils.export(new ExceptionDemoExService(), IDemoService.class, serviceURL);
    }

    public void initOrResetUrl(boolean isAsync) throws Exception {
        int port = NetUtils.getAvailablePort();
        consumerUrl = serviceURL = URL.valueOf("dubbo://127.0.0.1:" + port + "/" + IDemoService.class.getName() + "?group=" + System.nanoTime() + "&async=" + isAsync + "&timeout=100000&reference.filter=future");
    }

    public void initImplicitCallBackURL_onlyOnthrow() throws Exception {
        Map<String, Object> attitudes = new HashMap<>();
        ConsumerMethodModel.AsyncMethodInfo asyncMethodInfo = new ConsumerMethodModel.AsyncMethodInfo();
        asyncMethodInfo.setOnthrowInstance(notify);
        asyncMethodInfo.setOnthrowMethod(onThrowMethod);
        attitudes.put("get", asyncMethodInfo);
        ApplicationModel.initConsumerModel(consumerUrl.getServiceKey(), new ConsumerModel(consumerUrl.getServiceKey(), demoProxy, IDemoService.class.getMethods(), attitudes));
    }

    //================================================================================================

    public void initImplicitCallBackURL_onlyOnreturn() throws Exception {
        Map<String, Object> attitudes = new HashMap<>();
        ConsumerMethodModel.AsyncMethodInfo asyncMethodInfo = new ConsumerMethodModel.AsyncMethodInfo();
        asyncMethodInfo.setOnreturnInstance(notify);
        asyncMethodInfo.setOnreturnMethod(onReturnMethod);
        attitudes.put("get", asyncMethodInfo);
        ApplicationModel.initConsumerModel(consumerUrl.getServiceKey(), new ConsumerModel(consumerUrl.getServiceKey(), demoProxy, IDemoService.class.getMethods(), attitudes));
    }

    public void initImplicitCallBackURL_onlyOninvoke() throws Exception {
        Map<String, Object> attitudes = new HashMap<>();
        ConsumerMethodModel.AsyncMethodInfo asyncMethodInfo = new ConsumerMethodModel.AsyncMethodInfo();
        asyncMethodInfo.setOninvokeInstance(notify);
        asyncMethodInfo.setOninvokeMethod(onInvokeMethod);
        attitudes.put("get", asyncMethodInfo);
        ApplicationModel.initConsumerModel(consumerUrl.getServiceKey(), new ConsumerModel(consumerUrl.getServiceKey(), demoProxy, IDemoService.class.getMethods(), attitudes));
    }

    @Test
    public void test_CloseCallback() throws Exception {
        initOrResetUrl(false);
        initOrResetService();
        Person ret = demoProxy.get(1);
        Assert.assertEquals(1, ret.getId());
        destroyService();
    }

    @Test
    public void test_Sync_Onreturn() throws Exception {
        initOrResetUrl(false);
        initOrResetService();
        initImplicitCallBackURL_onlyOnreturn();

        int requestId = 2;
        Person ret = demoProxy.get(requestId);
        Assert.assertEquals(requestId, ret.getId());
        for (int i = 0; i < 10; i++) {
            if (!notify.ret.containsKey(requestId)) {
                Thread.sleep(200);
            } else {
                break;
            }
        }
        Assert.assertEquals(requestId, notify.ret.get(requestId).getId());
        destroyService();
    }

    @Test
    public void test_Ex_OnReturn() throws Exception {
        initOrResetUrl(true);
        initOrResetExService();
        initImplicitCallBackURL_onlyOnreturn();


        int requestId = 2;
        Person ret = demoProxy.get(requestId);
        Assert.assertEquals(null, ret);
        for (int i = 0; i < 10; i++) {
            if (!notify.errors.containsKey(requestId)) {
                Thread.sleep(200);
            } else {
                break;
            }
        }
        Assert.assertTrue(!notify.errors.containsKey(requestId));
        destroyService();
    }

    @Test
    public void test_Ex_OnInvoke() throws Exception {
        initOrResetUrl(true);
        initOrResetExService();
        initImplicitCallBackURL_onlyOninvoke();

        int requestId = 2;
        Person ret = demoProxy.get(requestId);
        Assert.assertEquals(null, ret);
        for (int i = 0; i < 10; i++) {
            if (!notify.inv.contains(requestId)) {
                Thread.sleep(200);
            } else {
                break;
            }
        }
        Assert.assertTrue(notify.inv.contains(requestId));
        destroyService();
    }

    @Test
    public void test_Ex_Onthrow() throws Exception {
        initOrResetUrl(true);
        initOrResetExService();
        initImplicitCallBackURL_onlyOnthrow();

        int requestId = 2;
        Person ret = demoProxy.get(requestId);
        Assert.assertEquals(null, ret);
        for (int i = 0; i < 10; i++) {
            if (!notify.errors.containsKey(requestId)) {
                Thread.sleep(200);
            } else {
                break;
            }
        }
        Assert.assertTrue(notify.errors.containsKey(requestId));
        Assert.assertTrue(notify.errors.get(requestId) instanceof Throwable);
        destroyService();
    }

    @Test
    public void test_Sync_NoFuture() throws Exception {
        initOrResetUrl(false);
        initOrResetService();
        initImplicitCallBackURL_onlyOnreturn();

        int requestId = 2;
        Person ret = demoProxy.get(requestId);
        Assert.assertEquals(requestId, ret.getId());
        Future<Person> pFuture = RpcContext.getContext().getFuture();
        Assert.assertEquals(null, pFuture);
        destroyService();
    }

    @Test
    public void test_Async_Future() throws Exception {
        initOrResetUrl(true);
        initOrResetService();

        int requestId = 2;
        Person ret = demoProxy.get(requestId);
        Assert.assertEquals(null, ret);
        Future<Person> pFuture = RpcContext.getContext().getFuture();
        ret = pFuture.get(1000 * 1000, TimeUnit.MICROSECONDS);
        Assert.assertEquals(requestId, ret.getId());
        destroyService();
    }

    @Test
    public void test_Async_Future_Multi() throws Exception {
        initOrResetUrl(true);
        initOrResetService();

        int requestId1 = 1;
        Person ret = demoProxy.get(requestId1);
        Assert.assertEquals(null, ret);
        Future<Person> p1Future = RpcContext.getContext().getFuture();

        int requestId2 = 1;
        Person ret2 = demoProxy.get(requestId2);
        Assert.assertEquals(null, ret2);
        Future<Person> p2Future = RpcContext.getContext().getFuture();

        ret = p1Future.get(1000 * 1000, TimeUnit.MICROSECONDS);
        ret2 = p2Future.get(1000 * 1000, TimeUnit.MICROSECONDS);
        Assert.assertEquals(requestId1, ret.getId());
        Assert.assertEquals(requestId2, ret.getId());
        destroyService();
    }

    @Test(expected = RuntimeException.class)
    public void test_Async_Future_Ex() throws Throwable {
        try {
            initOrResetUrl(true);
            initOrResetExService();

            int requestId = 2;
            Person ret = demoProxy.get(requestId);
            Assert.assertEquals(null, ret);
            Future<Person> pFuture = RpcContext.getContext().getFuture();
            ret = pFuture.get(1000 * 1000, TimeUnit.MICROSECONDS);
            Assert.assertEquals(requestId, ret.getId());
        } catch (ExecutionException e) {
            throw e.getCause();
        } finally {
            destroyService();
        }
    }

    @Test(expected = RuntimeException.class)
    public void test_Normal_Ex() throws Exception {
        initOrResetUrl(false);
        initOrResetExService();

        int requestId = 2;
        Person ret = demoProxy.get(requestId);
        Assert.assertEquals(requestId, ret.getId());
    }

    interface Nofify {
        public void onreturn(Person msg, Integer id);

        public void onthrow(Throwable ex, Integer id);

        public void oninvoke(Integer id);
    }

    interface IDemoService {
        public Person get(int id);
    }

    public static class Person implements Serializable {
        private static final long serialVersionUID = 1L;
        private int id;
        private String name;
        private int age;

        public Person(int id, String name, int age) {
            this.id = id;
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return "Person [name=" + name + ", age=" + age + "]";
        }
    }

    class NofifyImpl implements Nofify {
        public List<Integer> inv = new ArrayList<Integer>();
        public Map<Integer, Person> ret = new HashMap<Integer, Person>();
        public Map<Integer, Throwable> errors = new HashMap<Integer, Throwable>();
        public boolean exd = false;

        public void onreturn(Person msg, Integer id) {
            System.out.println("onNotify:" + msg);
            ret.put(id, msg);
        }

        public void onthrow(Throwable ex, Integer id) {
            errors.put(id, ex);
//            ex.printStackTrace();
        }

        public void oninvoke(Integer id) {
            inv.add(id);
        }
    }

    class NormalDemoService implements IDemoService {
        public Person get(int id) {
            return new Person(id, "charles", 4);
        }
    }

    class ExceptionDemoExService implements IDemoService {
        public Person get(int id) {
            throw new RuntimeException("request persion id is :" + id);
        }
    }
}