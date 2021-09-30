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
package org.apache.dubbo.rpc.protocol.injvm;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.compiler.support.CtClassBuilder;
import org.apache.dubbo.common.compiler.support.JavassistCompiler;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;

import demo.MultiClassLoaderService;
import demo.MultiClassLoaderServiceImpl;
import demo.MultiClassLoaderServiceRequest;
import demo.MultiClassLoaderServiceResult;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.NotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class InjvmClassLoaderTest {
    @Test
    public void testDifferentClassLoaderRequest() throws Exception {
        String basePath = DemoService.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        basePath = java.net.URLDecoder.decode(basePath, "UTF-8");
        TestClassLoader1 classLoader1 = new TestClassLoader1(basePath);
        TestClassLoader1 classLoader2 = new TestClassLoader1(basePath);
        TestClassLoader2 classLoader3 = new TestClassLoader2(classLoader2, basePath);

        ApplicationConfig applicationConfig = new ApplicationConfig("TestApp");
        ApplicationModel applicationModel = new ApplicationModel(FrameworkModel.defaultModel());
        applicationModel.getApplicationConfigManager().setApplication(applicationConfig);
        ModuleModel moduleModel = new ModuleModel(applicationModel);

        Class clazz1 = classLoader1.loadClass(MultiClassLoaderService.class.getName(), false);
        Class<?> clazz1impl = classLoader1.loadClass(MultiClassLoaderServiceImpl.class.getName(), false);
        Class<?> requestClazzCustom1 = compileCustomRequest(classLoader1);
        Class<?> resultClazzCustom1 = compileCustomResult(classLoader1);
        classLoader1.loadedClass.put(requestClazzCustom1.getName(), requestClazzCustom1);
        classLoader1.loadedClass.put(resultClazzCustom1.getName(), resultClazzCustom1);

        // AtomicReference to cache request/response of provider
        AtomicReference innerRequestReference = new AtomicReference();
        AtomicReference innerResultReference = new AtomicReference();
        innerResultReference.set(resultClazzCustom1.newInstance());
        Constructor<?> declaredConstructor = clazz1impl.getDeclaredConstructor(AtomicReference.class, AtomicReference.class);


        // export provider
        ProxyFactory proxyFactory = moduleModel.getExtensionLoader(ProxyFactory.class).getExtension("javassist");
        Protocol protocol = moduleModel.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        Object providerInstance = declaredConstructor.newInstance(innerRequestReference, innerResultReference);

        URL url = URL.valueOf("injvm://localhost:0/" + MultiClassLoaderServiceImpl.class.getName() + "?interface=" + MultiClassLoaderServiceImpl.class.getName());
        ServiceDescriptor providerServiceDescriptor = moduleModel.getServiceRepository().registerService(clazz1);
        ProviderModel providerModel = new ProviderModel(
            url.getServiceKey(),
            providerInstance,
            providerServiceDescriptor,
            null,
            null);
        providerModel.setClassLoader(classLoader1);

        URL providerUrl = url.setScopeModel(moduleModel).setServiceModel(providerModel);
        Invoker invoker = proxyFactory.getInvoker(providerInstance, clazz1, providerUrl);
        Exporter<?> exporter = protocol.export(invoker);

        Class<?> clazz2 = classLoader2.loadClass(MultiClassLoaderService.class.getName(), false);
        Class<?> requestClazzOrigin = classLoader2.loadClass(MultiClassLoaderServiceRequest.class.getName(), false);
        Class<?> requestClazzCustom2 = compileCustomRequest(classLoader2);
        Class<?> resultClazzCustom3 = compileCustomResult(classLoader3);
        classLoader2.loadedClass.put(requestClazzCustom2.getName(), requestClazzCustom2);
        classLoader3.loadedClass.put(resultClazzCustom3.getName(), resultClazzCustom3);

        // refer consumer
        ServiceDescriptor consumerServiceDescriptor = moduleModel.getServiceRepository().registerService(clazz2);
        ConsumerModel consumerModel = new ConsumerModel(clazz2.getName(), null, consumerServiceDescriptor, null,
            ApplicationModel.defaultModel().getDefaultModule(), null, null);
        consumerModel.setClassLoader(classLoader3);
        URL consumerUrl = url.setScopeModel(moduleModel).setServiceModel(consumerModel);

        Object object1 = proxyFactory.getProxy(protocol.refer(clazz2, consumerUrl));

        java.lang.reflect.Method callBean1 = object1.getClass().getDeclaredMethod("call", requestClazzOrigin);
        callBean1.setAccessible(true);
        Object result1 = callBean1.invoke(object1, requestClazzCustom2.newInstance());

        // invoke result should load from classLoader3 ( sub classLoader of classLoader2 --> consumer side classLoader)
        Assertions.assertEquals(resultClazzCustom3, result1.getClass());
        Assertions.assertNotEquals(classLoader2, result1.getClass().getClassLoader());

        // invoke reqeust param should load from classLoader1 ( provider side classLoader )
        Assertions.assertEquals(classLoader1, innerRequestReference.get().getClass().getClassLoader());

        exporter.unexport();
        applicationModel.destroy();
    }

    private Class<?> compileCustomRequest(ClassLoader classLoader) throws NotFoundException, CannotCompileException {
        CtClassBuilder builder = new CtClassBuilder();
        builder.setClassName(MultiClassLoaderServiceRequest.class.getName() + "A");
        builder.setSuperClassName(MultiClassLoaderServiceRequest.class.getName());
        CtClass cls = builder.build(classLoader);
        return cls.toClass(classLoader, JavassistCompiler.class.getProtectionDomain());
    }

    private Class<?> compileCustomResult(ClassLoader classLoader) throws NotFoundException, CannotCompileException {
        CtClassBuilder builder = new CtClassBuilder();
        builder.setClassName(MultiClassLoaderServiceResult.class.getName() + "A");
        builder.setSuperClassName(MultiClassLoaderServiceResult.class.getName());
        CtClass cls = builder.build(classLoader);
        return cls.toClass(classLoader, JavassistCompiler.class.getProtectionDomain());
    }

    private static class TestClassLoader1 extends ClassLoader {
        private String basePath;

        public TestClassLoader1(String basePath) {
            this.basePath = basePath;
        }

        Map<String, Class<?>> loadedClass = new ConcurrentHashMap<>();

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            try {
                byte[] bytes = loadClassData(name);
                return defineClass(name, bytes, 0, bytes.length);
            } catch (Exception e) {
                throw new ClassNotFoundException();
            }
        }

        @Override
        public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (loadedClass.containsKey(name)) {
                return loadedClass.get(name);
            }
            if (name.startsWith("demo")) {
                Class<?> aClass = this.findClass(name);
                this.loadedClass.put(name, aClass);
                if (resolve) {
                    this.resolveClass(aClass);
                }
                return aClass;
            } else {
                Class<?> loadedClass = this.findLoadedClass(name);
                if (loadedClass != null) {
                    return loadedClass;
                } else {
                    return super.loadClass(name, resolve);
                }
            }
        }


        public byte[] loadClassData(String className) throws IOException {
            className = className.replaceAll("\\.", "/");
            String path = basePath + File.separator + className + ".class";
            FileInputStream fileInputStream;
            byte[] classBytes;
            fileInputStream = new FileInputStream(path);
            int length = fileInputStream.available();
            classBytes = new byte[length];
            fileInputStream.read(classBytes);
            fileInputStream.close();
            return classBytes;
        }
    }

    private static class TestClassLoader2 extends ClassLoader {
        private String basePath;
        private TestClassLoader1 testClassLoader;

        Map<String, Class<?>> loadedClass = new ConcurrentHashMap<>();

        public TestClassLoader2(TestClassLoader1 testClassLoader, String basePath) {
            this.testClassLoader = testClassLoader;
            this.basePath = basePath;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            try {
                byte[] bytes = loadClassData(name);
                return defineClass(name, bytes, 0, bytes.length);
            } catch (Exception e) {
                throw new ClassNotFoundException();
            }
        }

        @Override
        public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (loadedClass.containsKey(name)) {
                return loadedClass.get(name);
            }
            if (name.startsWith("demo.MultiClassLoaderServiceRe")) {
                Class<?> aClass = this.findClass(name);
                this.loadedClass.put(name, aClass);
                if (resolve) {
                    this.resolveClass(aClass);
                }
                return aClass;
            } else {
                return testClassLoader.loadClass(name, resolve);
            }
        }


        public byte[] loadClassData(String className) throws IOException {
            className = className.replaceAll("\\.", "/");
            String path = basePath + File.separator + className + ".class";
            FileInputStream fileInputStream;
            byte[] classBytes;
            fileInputStream = new FileInputStream(path);
            int length = fileInputStream.available();
            classBytes = new byte[length];
            fileInputStream.read(classBytes);
            fileInputStream.close();
            return classBytes;
        }
    }
}
