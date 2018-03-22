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
package com.alibaba.dubbo.common.bytecode;

import com.alibaba.dubbo.common.utils.ClassHelper;
import com.alibaba.dubbo.common.utils.ReflectUtils;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Proxy.
 */
public abstract class Proxy {

    public static final InvocationHandler RETURN_NULL_INVOKER = new InvocationHandler() {
        public Object invoke(Object proxy, Method method, Object[] args) {
            return null;
        }
    };
    public static final InvocationHandler THROW_UNSUPPORTED_INVOKER = new InvocationHandler() {
        public Object invoke(Object proxy, Method method, Object[] args) {
            throw new UnsupportedOperationException("Method [" + ReflectUtils.getName(method) + "] unimplemented.");
        }
    };
    /**
     * Proxy Class 计数，用于生成 Proxy 类名自增。
     */
    private static final AtomicLong PROXY_CLASS_COUNTER = new AtomicLong(0);
    /**
     * 包名
     */
    private static final String PACKAGE_NAME = Proxy.class.getPackage().getName();
    /**
     * Proxy 对象缓存
     * key ：ClassLoader
     * value.key ：Proxy 标识。使用 Proxy 实现接口名拼接
     * value.value ：Proxy 对象
     */
    private static final Map<ClassLoader, Map<String, Object>> ProxyCacheMap = new WeakHashMap<ClassLoader, Map<String, Object>>();

    private static final Object PendingGenerationMarker = new Object();

    protected Proxy() {
    }

    /**
     * Get proxy.
     *
     * @param ics interface class array.
     * @return Proxy instance.
     */
    public static Proxy getProxy(Class<?>... ics) {
        return getProxy(ClassHelper.getClassLoader(Proxy.class), ics);
    }

    /**
     * Get proxy.
     *
     * @param cl  class loader.
     * @param ics interface class array.
     * @return Proxy instance.
     */
    public static Proxy getProxy(ClassLoader cl, Class<?>... ics) {
        // 校验接口超过上限
        if (ics.length > 65535)
            throw new IllegalArgumentException("interface limit exceeded");

        // use interface class name list as key.
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ics.length; i++) {
            String itf = ics[i].getName();
            // 校验是否为接口
            if (!ics[i].isInterface())
                throw new RuntimeException(itf + " is not a interface.");

            // 加载接口类
            Class<?> tmp = null;
            try {
                tmp = Class.forName(itf, false, cl);
            } catch (ClassNotFoundException e) {
            }

            if (tmp != ics[i]) // 加载接口类失败
                throw new IllegalArgumentException(ics[i] + " is not visible from class loader");

            sb.append(itf).append(';');
        }

        // use interface class name list as key.
        String key = sb.toString();

        // get cache by class loader.
        Map<String, Object> cache;
        synchronized (ProxyCacheMap) {
            cache = ProxyCacheMap.get(cl);
            if (cache == null) {
                cache = new HashMap<String, Object>();
                ProxyCacheMap.put(cl, cache);
            }
        }

        // 获得 Proxy
        Proxy proxy = null;
        synchronized (cache) {
            do {
                // 从缓存中获取 Proxy
                Object value = cache.get(key);
                if (value instanceof Reference<?>) {
                    proxy = (Proxy) ((Reference<?>) value).get();
                    if (proxy != null)
                        return proxy;
                }
                // 缓存中不存在，设置生成 Proxy 代码标记。创建中时，其他创建请求等待，避免并发。
                if (value == PendingGenerationMarker) {
                    try {
                        cache.wait();
                    } catch (InterruptedException e) {
                    }
                } else {
                    cache.put(key, PendingGenerationMarker);
                    break;
                }
            }
            while (true);
        }

        long id = PROXY_CLASS_COUNTER.getAndIncrement();
        String pkg = null;
        ClassGenerator ccp = null, // proxy class generator
                ccm = null; // Proxy class generator
        try {
            // 创建 proxy 代码生成器
            ccp = ClassGenerator.newInstance(cl);

            Set<String> worked = new HashSet<String>(); // 已处理方法签名集合。key：方法签名
            List<Method> methods = new ArrayList<Method>(); // 已处理方法集合。

            // 处理接口
            for (int i = 0; i < ics.length; i++) {
                // 非 public 接口，使用接口包名
                if (!Modifier.isPublic(ics[i].getModifiers())) {
                    String npkg = ics[i].getPackage().getName();
                    if (pkg == null) {
                        pkg = npkg;
                    } else {
                        if (!pkg.equals(npkg)) // 实现了两个非 public 的接口，
                            throw new IllegalArgumentException("non-public interfaces from different packages");
                    }
                }
                // 添加接口
                ccp.addInterface(ics[i]);

                // 处理接口方法
                for (Method method : ics[i].getMethods()) {
                    // 添加方法签名到已处理方法签名集合
                    String desc = ReflectUtils.getDesc(method);
                    if (worked.contains(desc))
                        continue;
                    worked.add(desc);

                    // 生成接口方法实现代码
                    int ix = methods.size();
                    Class<?> rt = method.getReturnType();
                    Class<?>[] pts = method.getParameterTypes();

                    StringBuilder code = new StringBuilder("Object[] args = new Object[").append(pts.length).append("];");
                    for (int j = 0; j < pts.length; j++)
                        code.append(" args[").append(j).append("] = ($w)$").append(j + 1).append(";");
                    code.append(" Object ret = handler.invoke(this, methods[" + ix + "], args);");
                    if (!Void.TYPE.equals(rt))
                        code.append(" return ").append(asArgument(rt, "ret")).append(";");

                    methods.add(method);
                    // 添加方法
                    ccp.addMethod(method.getName(), method.getModifiers(), rt, pts, method.getExceptionTypes(), code.toString());
                }
            }

            // 设置包路径
            if (pkg == null)
                pkg = PACKAGE_NAME;

            // ===== 设置 proxy 代码生成的属性 =====
            // create ProxyInstance class.
            // 设置类名
            String pcn = pkg + ".proxy" + id;
            ccp.setClassName(pcn);
            // 添加静态属性 methods
            ccp.addField("public static java.lang.reflect.Method[] methods;");
            // 添加属性 handler
            ccp.addField("private " + InvocationHandler.class.getName() + " handler;");
            // 添加构造方法，参数 handler
            ccp.addConstructor(Modifier.PUBLIC, new Class<?>[]{InvocationHandler.class}, new Class<?>[0], "handler=$1;");
            // 添加构造方法，参数 空
            ccp.addDefaultConstructor();
            // 生成类
            Class<?> clazz = ccp.toClass();
            // 设置静态属性 methods
            clazz.getField("methods").set(null, methods.toArray(new Method[0]));

            // create Proxy class.
            // 创建 Proxy 代码生成器
            String fcn = Proxy.class.getName() + id;
            // 设置类名
            ccm = ClassGenerator.newInstance(cl);
            ccm.setClassName(fcn);
            // 添加构造方法，参数 空
            ccm.addDefaultConstructor();
            // 设置父类为 Proxy.class
            ccm.setSuperClass(Proxy.class);
            // 添加方法 #newInstance(handler)
            ccm.addMethod("public Object newInstance(" + InvocationHandler.class.getName() + " h){ return new " + pcn + "($1); }");
            // 生成类
            Class<?> pc = ccm.toClass();
            // 创建 Proxy 对象
            proxy = (Proxy) pc.newInstance();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            // release ClassGenerator
            if (ccp != null)
                ccp.release();
            if (ccm != null)
                ccm.release();
            // 唤醒缓存 wait
            synchronized (cache) {
                if (proxy == null)
                    cache.remove(key);
                else
                    cache.put(key, new WeakReference<Proxy>(proxy));
                cache.notifyAll();
            }
        }
        return proxy;
    }

    private static String asArgument(Class<?> cl, String name) {
        if (cl.isPrimitive()) {
            if (Boolean.TYPE == cl)
                return name + "==null?false:((Boolean)" + name + ").booleanValue()";
            if (Byte.TYPE == cl)
                return name + "==null?(byte)0:((Byte)" + name + ").byteValue()";
            if (Character.TYPE == cl)
                return name + "==null?(char)0:((Character)" + name + ").charValue()";
            if (Double.TYPE == cl)
                return name + "==null?(double)0:((Double)" + name + ").doubleValue()";
            if (Float.TYPE == cl)
                return name + "==null?(float)0:((Float)" + name + ").floatValue()";
            if (Integer.TYPE == cl)
                return name + "==null?(int)0:((Integer)" + name + ").intValue()";
            if (Long.TYPE == cl)
                return name + "==null?(long)0:((Long)" + name + ").longValue()";
            if (Short.TYPE == cl)
                return name + "==null?(short)0:((Short)" + name + ").shortValue()";
            throw new RuntimeException(name + " is unknown primitive type.");
        }
        return "(" + ReflectUtils.getName(cl) + ")" + name;
    }

    /**
     * get instance with default handler.
     *
     * @return instance.
     */
    public Object newInstance() {
        return newInstance(THROW_UNSUPPORTED_INVOKER);
    }

    /**
     * get instance with special handler.
     *
     * @return instance.
     */
    abstract public Object newInstance(InvocationHandler handler);
}