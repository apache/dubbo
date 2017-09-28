/*
 * Copyright 1999-2012 Alibaba Group.
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
package com.alibaba.dubbo.config;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.beanutil.JavaBeanAccessor;
import com.alibaba.dubbo.common.beanutil.JavaBeanDescriptor;
import com.alibaba.dubbo.common.beanutil.JavaBeanSerializeUtil;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.serialize.Serialization;
import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.alibaba.dubbo.config.api.DemoException;
import com.alibaba.dubbo.config.api.DemoService;
import com.alibaba.dubbo.config.api.User;
import com.alibaba.dubbo.config.provider.impl.DemoServiceImpl;
import com.alibaba.dubbo.rpc.service.GenericException;
import com.alibaba.dubbo.rpc.service.GenericService;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * GenericServiceTest
 *
 * @author william.liangf
 */
public class GenericServiceTest {

    @Test
    public void testGenericServiceException() {
        ServiceConfig<GenericService> service = new ServiceConfig<GenericService>();
        service.setApplication(new ApplicationConfig("generic-provider"));
        service.setRegistry(new RegistryConfig("N/A"));
        service.setProtocol(new ProtocolConfig("dubbo", 29581));
        service.setInterface(DemoService.class.getName());
        service.setRef(new GenericService() {

            public Object $invoke(String method, String[] parameterTypes, Object[] args)
                    throws GenericException {
                if ("sayName".equals(method)) {
                    return "Generic " + args[0];
                }
                if ("throwDemoException".equals(method)) {
                    throw new GenericException(DemoException.class.getName(), "Generic");
                }
                if ("getUsers".equals(method)) {
                    return args[0];
                }
                return null;
            }
        });
        service.export();
        try {
            ReferenceConfig<DemoService> reference = new ReferenceConfig<DemoService>();
            reference.setApplication(new ApplicationConfig("generic-consumer"));
            reference.setInterface(DemoService.class);
            reference.setUrl("dubbo://127.0.0.1:29581?generic=true");
            DemoService demoService = reference.get();
            try {
                // say name
                Assert.assertEquals("Generic Haha", demoService.sayName("Haha"));
                // get users
                List<User> users = new ArrayList<User>();
                users.add(new User("Aaa"));
                users = demoService.getUsers(users);
                Assert.assertEquals("Aaa", users.get(0).getName());
                // throw demo exception
                try {
                    demoService.throwDemoException();
                    Assert.fail();
                } catch (DemoException e) {
                    Assert.assertEquals("Generic", e.getMessage());
                }
            } finally {
                reference.destroy();
            }
        } finally {
            service.unexport();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGenericReferenceException() {
        ServiceConfig<DemoService> service = new ServiceConfig<DemoService>();
        service.setApplication(new ApplicationConfig("generic-provider"));
        service.setRegistry(new RegistryConfig("N/A"));
        service.setProtocol(new ProtocolConfig("dubbo", 29581));
        service.setInterface(DemoService.class.getName());
        service.setRef(new DemoServiceImpl());
        service.export();
        try {
            ReferenceConfig<GenericService> reference = new ReferenceConfig<GenericService>();
            reference.setApplication(new ApplicationConfig("generic-consumer"));
            reference.setInterface(DemoService.class);
            reference.setUrl("dubbo://127.0.0.1:29581?scope=remote");
            reference.setGeneric(true);
            GenericService genericService = reference.get();
            try {
                List<Map<String, Object>> users = new ArrayList<Map<String, Object>>();
                Map<String, Object> user = new HashMap<String, Object>();
                user.put("class", "com.alibaba.dubbo.config.api.User");
                user.put("name", "actual.provider");
                users.add(user);
                users = (List<Map<String, Object>>) genericService.$invoke("getUsers", new String[]{List.class.getName()}, new Object[]{users});
                Assert.assertEquals(1, users.size());
                Assert.assertEquals("actual.provider", users.get(0).get("name"));
            } finally {
                reference.destroy();
            }
        } finally {
            service.unexport();
        }
    }

    @Test
    public void testGenericSerializationJava() throws Exception {
        ServiceConfig<DemoService> service = new ServiceConfig<DemoService>();
        service.setApplication(new ApplicationConfig("generic-provider"));
        service.setRegistry(new RegistryConfig("N/A"));
        service.setProtocol(new ProtocolConfig("dubbo", 29581));
        service.setInterface(DemoService.class.getName());
        DemoServiceImpl ref = new DemoServiceImpl();
        service.setRef(ref);
        service.export();
        try {
            ReferenceConfig<GenericService> reference = new ReferenceConfig<GenericService>();
            reference.setApplication(new ApplicationConfig("generic-consumer"));
            reference.setInterface(DemoService.class);
            reference.setUrl("dubbo://127.0.0.1:29581?scope=remote");
            reference.setGeneric(Constants.GENERIC_SERIALIZATION_NATIVE_JAVA);
            GenericService genericService = reference.get();
            try {
                String name = "kimi";
                ByteArrayOutputStream bos = new ByteArrayOutputStream(512);
                ExtensionLoader.getExtensionLoader(Serialization.class)
                        .getExtension("nativejava").serialize(null, bos).writeObject(name);
                byte[] arg = bos.toByteArray();
                Object obj = genericService.$invoke("sayName", new String[]{String.class.getName()}, new Object[]{arg});
                Assert.assertTrue(obj instanceof byte[]);
                byte[] result = (byte[]) obj;
                Assert.assertEquals(ref.sayName(name), ExtensionLoader.getExtensionLoader(Serialization.class)
                        .getExtension("nativejava").deserialize(null, new ByteArrayInputStream(result)).readObject().toString());

                // getUsers
                List<User> users = new ArrayList<User>();
                User user = new User();
                user.setName(name);
                users.add(user);
                bos = new ByteArrayOutputStream(512);
                ExtensionLoader.getExtensionLoader(Serialization.class)
                        .getExtension("nativejava").serialize(null, bos).writeObject(users);
                obj = genericService.$invoke("getUsers",
                        new String[]{List.class.getName()},
                        new Object[]{bos.toByteArray()});
                Assert.assertTrue(obj instanceof byte[]);
                result = (byte[]) obj;
                Assert.assertEquals(users,
                        ExtensionLoader.getExtensionLoader(Serialization.class)
                                .getExtension("nativejava")
                                .deserialize(null, new ByteArrayInputStream(result))
                                .readObject());

                // echo(int)
                bos = new ByteArrayOutputStream(512);
                ExtensionLoader.getExtensionLoader(Serialization.class).getExtension("nativejava")
                        .serialize(null, bos).writeObject(Integer.MAX_VALUE);
                obj = genericService.$invoke("echo", new String[]{int.class.getName()}, new Object[]{bos.toByteArray()});
                Assert.assertTrue(obj instanceof byte[]);
                Assert.assertEquals(Integer.MAX_VALUE,
                        ExtensionLoader.getExtensionLoader(Serialization.class)
                                .getExtension("nativejava")
                                .deserialize(null, new ByteArrayInputStream((byte[]) obj))
                                .readObject());

            } finally {
                reference.destroy();
            }
        } finally {
            service.unexport();
        }
    }

    @Test
    public void testGenericInvokeWithBeanSerialization() throws Exception {
        ServiceConfig<DemoService> service = new ServiceConfig<DemoService>();
        service.setApplication(new ApplicationConfig("bean-provider"));
        service.setInterface(DemoService.class);
        service.setRegistry(new RegistryConfig("N/A"));
        DemoServiceImpl impl = new DemoServiceImpl();
        service.setRef(impl);
        service.setProtocol(new ProtocolConfig("dubbo", 29581));
        service.export();
        ReferenceConfig<GenericService> reference = null;
        try {
            reference = new ReferenceConfig<GenericService>();
            reference.setApplication(new ApplicationConfig("bean-consumer"));
            reference.setInterface(DemoService.class);
            reference.setUrl("dubbo://127.0.0.1:29581?scope=remote");
            reference.setGeneric(Constants.GENERIC_SERIALIZATION_BEAN);
            GenericService genericService = reference.get();
            User user = new User();
            user.setName("zhangsan");
            List<User> users = new ArrayList<User>();
            users.add(user);
            Object result = genericService.$invoke("getUsers", new String[]{ReflectUtils.getName(List.class)}, new Object[]{JavaBeanSerializeUtil.serialize(users, JavaBeanAccessor.METHOD)});
            Assert.assertTrue(result instanceof JavaBeanDescriptor);
            JavaBeanDescriptor descriptor = (JavaBeanDescriptor) result;
            Assert.assertTrue(descriptor.isCollectionType());
            Assert.assertEquals(1, descriptor.propertySize());
            descriptor = (JavaBeanDescriptor) descriptor.getProperty(0);
            Assert.assertTrue(descriptor.isBeanType());
            Assert.assertEquals(user.getName(), ((JavaBeanDescriptor) descriptor.getProperty("name")).getPrimitiveProperty());
        } finally {
            if (reference != null) {
                reference.destroy();
            }
            service.unexport();
        }
    }

    @Test
    public void testGenericImplementationWithBeanSerialization() throws Exception {
        final AtomicReference reference = new AtomicReference();
        ServiceConfig<GenericService> service = new ServiceConfig<GenericService>();
        service.setApplication(new ApplicationConfig("bean-provider"));
        service.setRegistry(new RegistryConfig("N/A"));
        service.setProtocol(new ProtocolConfig("dubbo", 29581));
        service.setInterface(DemoService.class.getName());
        service.setRef(new GenericService() {

            public Object $invoke(String method, String[] parameterTypes, Object[] args) throws GenericException {
                if ("getUsers".equals(method)) {
                    GenericParameter arg = new GenericParameter();
                    arg.method = method;
                    arg.parameterTypes = parameterTypes;
                    arg.arguments = args;
                    reference.set(arg);
                    return args[0];
                }
                if ("sayName".equals(method)) {
                    return null;
                }
                return args;
            }
        });
        service.export();
        ReferenceConfig<DemoService> ref = null;
        try {
            ref = new ReferenceConfig<DemoService>();
            ref.setApplication(new ApplicationConfig("bean-consumer"));
            ref.setInterface(DemoService.class);
            ref.setUrl("dubbo://127.0.0.1:29581?scope=remote&generic=bean");
            DemoService demoService = ref.get();
            User user = new User();
            user.setName("zhangsan");
            List<User> users = new ArrayList<User>();
            users.add(user);
            List<User> result = demoService.getUsers(users);
            Assert.assertEquals(users.size(), result.size());
            Assert.assertEquals(user.getName(), result.get(0).getName());

            GenericParameter gp = (GenericParameter) reference.get();
            Assert.assertEquals("getUsers", gp.method);
            Assert.assertEquals(1, gp.parameterTypes.length);
            Assert.assertEquals(ReflectUtils.getName(List.class), gp.parameterTypes[0]);
            Assert.assertEquals(1, gp.arguments.length);
            Assert.assertTrue(gp.arguments[0] instanceof JavaBeanDescriptor);
            JavaBeanDescriptor descriptor = (JavaBeanDescriptor) gp.arguments[0];
            Assert.assertTrue(descriptor.isCollectionType());
            Assert.assertEquals(ArrayList.class.getName(), descriptor.getClassName());
            Assert.assertEquals(1, descriptor.propertySize());
            descriptor = (JavaBeanDescriptor) descriptor.getProperty(0);
            Assert.assertTrue(descriptor.isBeanType());
            Assert.assertEquals(User.class.getName(), descriptor.getClassName());
            Assert.assertEquals(user.getName(), ((JavaBeanDescriptor) descriptor.getProperty("name")).getPrimitiveProperty());
            Assert.assertNull(demoService.sayName("zhangsan"));
        } finally {
            if (ref != null) {
                ref.destroy();
            }
            service.unexport();
        }
    }

    protected static class GenericParameter {

        String method;

        String[] parameterTypes;

        Object[] arguments;
    }

}
