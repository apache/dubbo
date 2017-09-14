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
package com.alibaba.dubbo.config;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * @author haomin.liuhm
 */
public class RpcConfigGetSetProxy {

    private static final String RPC_CONFIG_BASECLASS = AbstractConfig.class.getName();
    private static final Logger log = LoggerFactory.getLogger(RpcConfigGetSetProxy.class);


    private Object proxiee = null;
    private Class<?> proxieeClass = null;
    private Boolean isOk = false;

    public RpcConfigGetSetProxy(Object p) {

        if (p == null) {
            return;
        }

        if (!isKindOf(p.getClass(), RPC_CONFIG_BASECLASS)) {
            return;
        }

        proxiee = p;
        //proxieeClass = c;
        proxieeClass = p.getClass();
        isOk = true;

    }

    public static boolean isKindOf(Class<?> c, String type) {

        // get the class def for obj and type

        Class<?> tClass;
        try {
            tClass = Class.forName(type);
        } catch (ClassNotFoundException e) {
            return false;
        }

        // check against type and superclasses
        while (c != null) {
            if (c == tClass) return true;
            c = c.getSuperclass();
        }

        return false;
    }

    public boolean isOk() {
        return isOk;
    }

    public Object setValue(String key, Object value) {

        if (!isOk()) {
            return null;
        }

        Method m = findSetMethod(key, value, proxieeClass);
        return invoke(m, value);
    }

    public Object getValue(String key) {

        if (!isOk()) {
            return null;
        }

        Method m = findGetMethod(key, proxieeClass);
        return invoke(m, null);
    }

    private Object invoke(Method m, Object value) {

        if (m == null) {
            return null;
        }

        try {
            if (value == null) {
                return m.invoke(proxiee, (Object[]) null);
            } else {
                return m.invoke(proxiee, value);
            }
        } catch (IllegalArgumentException e) {
            log.error("IllegalArgumentException", e);
            return null;
        } catch (IllegalAccessException e) {
            log.error("IllegalAccessException", e);
            return null;
        } catch (InvocationTargetException e) {
            log.error("InvocationTargetException", e);
            return null;
        }
    }

    private Method findGetMethod(String key, Class<?> clazz) {

        Method m = findMethod(key, null, "get", clazz);
        if (m != null) {
            return m;
        }

        return findMethod(key, null, "is", clazz);
    }

    private Method findSetMethod(String key, Object value, Class<?> clazz) {

        return findMethod(key, value, "set", clazz);
    }

    private Method getMethod(String methodName, Object value, Class<?> clazz) {

        try {
            if (value == null) {
                return clazz.getMethod(methodName, (Class<?>[]) null);
            } else {
                return clazz.getMethod(methodName, value.getClass());
            }
        } catch (SecurityException e) {
            log.error("SecurityException: " + e.getMessage());
            return null;
        } catch (NoSuchMethodException e) {
            log.error("NoSuchMethodException: " + e.getMessage());
            return null;
        }
    }

    private Method findMethod(String key, Object value, String prefix, Class<?> clazz) {

        if (key.length() < 2) {
            return null;
        }

        key = key.substring(0, 1).toUpperCase() + key.substring(1);
        String methodName = prefix + key;

        return getMethod(methodName, value, clazz);
    }

}