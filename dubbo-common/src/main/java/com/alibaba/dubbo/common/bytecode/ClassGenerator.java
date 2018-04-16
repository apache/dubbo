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

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.LoaderClassPath;
import javassist.NotFoundException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ClassGenerator
 *
 * 类生成器，基于 Javassist 实现。
 */
public final class ClassGenerator {

    private static final AtomicLong CLASS_NAME_COUNTER = new AtomicLong(0);
    private static final String SIMPLE_NAME_TAG = "<init>";
    private static final Map<ClassLoader, ClassPool> POOL_MAP = new ConcurrentHashMap<ClassLoader, ClassPool>(); //ClassLoader - ClassPool

    /**
     * CtClass hash 集合
     * key：类名
     */
    private ClassPool mPool;
    /**
     * CtClass 对象
     *
     * 使用 {@link #mPool} 生成
     */
    private CtClass mCtc;
    /**
     * 生成类的类名
     */
    private String mClassName;
    /**
     * 生成类的父类
     */
    private String mSuperClass;
    /**
     * 生成类的接口集合
     */
    private Set<String> mInterfaces;
    /**
     * 生成类的属性集合
     */
    private List<String> mFields;
    /**
     * 生成类的非空构造方法代码集合
     */
    private List<String> mConstructors;
    /**
     * 生成类的方法代码集合
     */
    private List<String> mMethods;
    private Map<String, Method> mCopyMethods; // <method desc,method instance>
    private Map<String, Constructor<?>> mCopyConstructors; // <constructor desc,constructor instance>
    /**
     * 默认空构造方法
     */
    private boolean mDefaultConstructor = false;

    private ClassGenerator() {
    }

    private ClassGenerator(ClassPool pool) {
        mPool = pool;
    }

    public static ClassGenerator newInstance() {
        return new ClassGenerator(getClassPool(Thread.currentThread().getContextClassLoader()));
    }

    public static ClassGenerator newInstance(ClassLoader loader) {
        return new ClassGenerator(getClassPool(loader));
    }

    public static boolean isDynamicClass(Class<?> cl) {
        return ClassGenerator.DC.class.isAssignableFrom(cl);
    }

    public static ClassPool getClassPool(ClassLoader loader) {
        if (loader == null)
            return ClassPool.getDefault();

        ClassPool pool = POOL_MAP.get(loader);
        if (pool == null) {
            pool = new ClassPool(true);
            pool.appendClassPath(new LoaderClassPath(loader));
            POOL_MAP.put(loader, pool);
        }
        return pool;
    }

    private static String modifier(int mod) {
        if (Modifier.isPublic(mod)) return "public";
        if (Modifier.isProtected(mod)) return "protected";
        if (Modifier.isPrivate(mod)) return "private";
        return "";
    }

    public String getClassName() {
        return mClassName;
    }

    public ClassGenerator setClassName(String name) {
        mClassName = name;
        return this;
    }

    public ClassGenerator addInterface(String cn) {
        if (mInterfaces == null)
            mInterfaces = new HashSet<String>();
        mInterfaces.add(cn);
        return this;
    }

    public ClassGenerator addInterface(Class<?> cl) {
        return addInterface(cl.getName());
    }

    public ClassGenerator setSuperClass(String cn) {
        mSuperClass = cn;
        return this;
    }

    public ClassGenerator setSuperClass(Class<?> cl) {
        mSuperClass = cl.getName();
        return this;
    }

    public ClassGenerator addField(String code) {
        if (mFields == null)
            mFields = new ArrayList<String>();
        mFields.add(code);
        return this;
    }

    public ClassGenerator addField(String name, int mod, Class<?> type) {
        return addField(name, mod, type, null);
    }

    public ClassGenerator addField(String name, int mod, Class<?> type, String def) {
        StringBuilder sb = new StringBuilder();
        sb.append(modifier(mod)).append(' ').append(ReflectUtils.getName(type)).append(' ');
        sb.append(name);
        if (def != null && def.length() > 0) {
            sb.append('=');
            sb.append(def);
        }
        sb.append(';');
        return addField(sb.toString());
    }

    public ClassGenerator addMethod(String code) {
        if (mMethods == null)
            mMethods = new ArrayList<String>();
        mMethods.add(code);
        return this;
    }

    public ClassGenerator addMethod(String name, int mod, Class<?> rt, Class<?>[] pts, String body) {
        return addMethod(name, mod, rt, pts, null, body);
    }

    public ClassGenerator addMethod(String name, int mod, Class<?> rt, Class<?>[] pts, Class<?>[] ets, String body) {
        StringBuilder sb = new StringBuilder();
        sb.append(modifier(mod)).append(' ').append(ReflectUtils.getName(rt)).append(' ').append(name);
        sb.append('(');
        for (int i = 0; i < pts.length; i++) {
            if (i > 0)
                sb.append(',');
            sb.append(ReflectUtils.getName(pts[i]));
            sb.append(" arg").append(i);
        }
        sb.append(')');
        if (ets != null && ets.length > 0) {
            sb.append(" throws ");
            for (int i = 0; i < ets.length; i++) {
                if (i > 0)
                    sb.append(',');
                sb.append(ReflectUtils.getName(ets[i]));
            }
        }
        sb.append('{').append(body).append('}');
        return addMethod(sb.toString());
    }

    public ClassGenerator addMethod(Method m) {
        addMethod(m.getName(), m);
        return this;
    }

    public ClassGenerator addMethod(String name, Method m) {
        String desc = name + ReflectUtils.getDescWithoutMethodName(m);
        addMethod(':' + desc);
        if (mCopyMethods == null)
            mCopyMethods = new ConcurrentHashMap<String, Method>(8);
        mCopyMethods.put(desc, m);
        return this;
    }

    public ClassGenerator addConstructor(String code) {
        if (mConstructors == null)
            mConstructors = new LinkedList<String>();
        mConstructors.add(code);
        return this;
    }

    public ClassGenerator addConstructor(int mod, Class<?>[] pts, String body) {
        return addConstructor(mod, pts, null, body);
    }

    public ClassGenerator addConstructor(int mod, Class<?>[] pts, Class<?>[] ets, String body) {
        // 构造方法代码
        StringBuilder sb = new StringBuilder();
        sb.append(modifier(mod)).append(' ').append(SIMPLE_NAME_TAG);
        sb.append('(');
        for (int i = 0; i < pts.length; i++) {
            if (i > 0)
                sb.append(',');
            sb.append(ReflectUtils.getName(pts[i]));
            sb.append(" arg").append(i);
        }
        sb.append(')');
        if (ets != null && ets.length > 0) {
            sb.append(" throws ");
            for (int i = 0; i < ets.length; i++) {
                if (i > 0)
                    sb.append(',');
                sb.append(ReflectUtils.getName(ets[i]));
            }
        }
        sb.append('{').append(body).append('}');
        //
        return addConstructor(sb.toString());
    }

    public ClassGenerator addConstructor(Constructor<?> c) {
        String desc = ReflectUtils.getDesc(c);
        addConstructor(":" + desc);
        if (mCopyConstructors == null)
            mCopyConstructors = new ConcurrentHashMap<String, Constructor<?>>(4);
        mCopyConstructors.put(desc, c);
        return this;
    }

    public ClassGenerator addDefaultConstructor() {
        mDefaultConstructor = true;
        return this;
    }

    public ClassPool getClassPool() {
        return mPool;
    }

    public Class<?> toClass() {
        return toClass(ClassHelper.getClassLoader(ClassGenerator.class), getClass().getProtectionDomain());
    }

    public Class<?> toClass(ClassLoader loader, ProtectionDomain pd) {
        // mCtc 非空时，进行释放；下面会进行创建 mCtc
        if (mCtc != null)
            mCtc.detach();
        long id = CLASS_NAME_COUNTER.getAndIncrement();
        try {
            CtClass ctcs = mSuperClass == null ? null : mPool.get(mSuperClass);
            // 类名
            if (mClassName == null)
                mClassName = (mSuperClass == null || javassist.Modifier.isPublic(ctcs.getModifiers())
                        ? ClassGenerator.class.getName() : mSuperClass + "$sc") + id;
            // 创建 mCtc
            mCtc = mPool.makeClass(mClassName);
            // 继承类
            if (mSuperClass != null)
                mCtc.setSuperclass(ctcs);
            // add dynamic class tag.
            mCtc.addInterface(mPool.get(DC.class.getName())); // add dynamic class tag.
            // 实现接口集合
            if (mInterfaces != null)
                for (String cl : mInterfaces) mCtc.addInterface(mPool.get(cl));
            // 属性集合
            if (mFields != null)
                for (String code : mFields) mCtc.addField(CtField.make(code, mCtc));
            // 方法集合
            if (mMethods != null) {
                for (String code : mMethods) {
                    if (code.charAt(0) == ':')
                        mCtc.addMethod(CtNewMethod.copy(getCtMethod(mCopyMethods.get(code.substring(1))), code.substring(1, code.indexOf('(')), mCtc, null));
                    else
                        mCtc.addMethod(CtNewMethod.make(code, mCtc));
                }
            }
            // 空参数构造方法
            if (mDefaultConstructor)
                mCtc.addConstructor(CtNewConstructor.defaultConstructor(mCtc));
            // 带参数构造方法
            if (mConstructors != null) {
                for (String code : mConstructors) {
                    if (code.charAt(0) == ':') {
                        mCtc.addConstructor(CtNewConstructor.copy(getCtConstructor(mCopyConstructors.get(code.substring(1))), mCtc, null));
                    } else {
                        String[] sn = mCtc.getSimpleName().split("\\$+"); // inner class name include $.
                        mCtc.addConstructor(CtNewConstructor.make(code.replaceFirst(SIMPLE_NAME_TAG, sn[sn.length - 1]), mCtc));
                    }
                }
            }
//            mCtc.debugWriteFile("/Users/yunai/test/" + mCtc.getSimpleName().replaceAll(".", "/") + ".class");
//            mCtc.debugWriteFile("/Users/yunai/test/" + mCtc.getSimpleName() + ".class");
            // 生成
            return mCtc.toClass(loader, pd);
        } catch (RuntimeException e) {
            throw e;
        } catch (NotFoundException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (CannotCompileException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * 释放
     */
    public void release() {
        if (mCtc != null) mCtc.detach();
        if (mInterfaces != null) mInterfaces.clear();
        if (mFields != null) mFields.clear();
        if (mMethods != null) mMethods.clear();
        if (mConstructors != null) mConstructors.clear();
        if (mCopyMethods != null) mCopyMethods.clear();
        if (mCopyConstructors != null) mCopyConstructors.clear();
    }

    private CtClass getCtClass(Class<?> c) throws NotFoundException {
        return mPool.get(c.getName());
    }

    private CtMethod getCtMethod(Method m) throws NotFoundException {
        return getCtClass(m.getDeclaringClass()).getMethod(m.getName(), ReflectUtils.getDescWithoutMethodName(m));
    }

    private CtConstructor getCtConstructor(Constructor<?> c) throws NotFoundException {
        return getCtClass(c.getDeclaringClass()).getConstructor(ReflectUtils.getDesc(c));
    }

    /**
     * 动态编译接口，用于标记类是通过 {@link #ClassGenerator} 生成的
     */
    public static interface DC {
    } // dynamic class tag interface.
}