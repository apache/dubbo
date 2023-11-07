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
package org.apache.dubbo.common.compact;

import org.apache.dubbo.rpc.service.GenericException;

import java.lang.reflect.Constructor;

public class Dubbo2GenericExceptionUtils {
    private static final Class<? extends org.apache.dubbo.rpc.service.GenericException> GENERIC_EXCEPTION_CLASS;
    private static final Constructor<? extends org.apache.dubbo.rpc.service.GenericException>
            GENERIC_EXCEPTION_CONSTRUCTOR;
    private static final Constructor<? extends org.apache.dubbo.rpc.service.GenericException>
            GENERIC_EXCEPTION_CONSTRUCTOR_S;
    private static final Constructor<? extends org.apache.dubbo.rpc.service.GenericException>
            GENERIC_EXCEPTION_CONSTRUCTOR_S_S;
    private static final Constructor<? extends org.apache.dubbo.rpc.service.GenericException>
            GENERIC_EXCEPTION_CONSTRUCTOR_T;
    private static final Constructor<? extends org.apache.dubbo.rpc.service.GenericException>
            GENERIC_EXCEPTION_CONSTRUCTOR_S_T_S_S;

    static {
        GENERIC_EXCEPTION_CLASS = loadClass();
        GENERIC_EXCEPTION_CONSTRUCTOR = loadConstructor();
        GENERIC_EXCEPTION_CONSTRUCTOR_S = loadConstructor(String.class);
        GENERIC_EXCEPTION_CONSTRUCTOR_S_S = loadConstructor(String.class, String.class);
        GENERIC_EXCEPTION_CONSTRUCTOR_T = loadConstructor(Throwable.class);
        GENERIC_EXCEPTION_CONSTRUCTOR_S_T_S_S =
                loadConstructor(String.class, Throwable.class, String.class, String.class);
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends org.apache.dubbo.rpc.service.GenericException> loadClass() {
        try {
            Class<?> clazz = Class.forName("com.alibaba.dubbo.rpc.service.GenericException");
            if (GenericException.class.isAssignableFrom(clazz)) {
                return (Class<? extends org.apache.dubbo.rpc.service.GenericException>) clazz;
            } else {
                return null;
            }
        } catch (Throwable e) {
            return null;
        }
    }

    private static Constructor<? extends org.apache.dubbo.rpc.service.GenericException> loadConstructor(
            Class<?>... parameterTypes) {
        if (GENERIC_EXCEPTION_CLASS == null) {
            return null;
        }
        try {
            return GENERIC_EXCEPTION_CLASS.getConstructor(parameterTypes);
        } catch (Throwable e) {
            return null;
        }
    }

    public static boolean isGenericExceptionClassLoaded() {
        return GENERIC_EXCEPTION_CLASS != null
                && GENERIC_EXCEPTION_CONSTRUCTOR != null
                && GENERIC_EXCEPTION_CONSTRUCTOR_S != null
                && GENERIC_EXCEPTION_CONSTRUCTOR_S_S != null
                && GENERIC_EXCEPTION_CONSTRUCTOR_T != null
                && GENERIC_EXCEPTION_CONSTRUCTOR_S_T_S_S != null;
    }

    public static Class<? extends org.apache.dubbo.rpc.service.GenericException> getGenericExceptionClass() {
        return GENERIC_EXCEPTION_CLASS;
    }

    public static org.apache.dubbo.rpc.service.GenericException newGenericException() {
        if (GENERIC_EXCEPTION_CONSTRUCTOR == null) {
            return null;
        }
        try {
            return GENERIC_EXCEPTION_CONSTRUCTOR.newInstance();
        } catch (Throwable e) {
            return null;
        }
    }

    public static org.apache.dubbo.rpc.service.GenericException newGenericException(String exceptionMessage) {
        if (GENERIC_EXCEPTION_CONSTRUCTOR_S == null) {
            return null;
        }
        try {
            return GENERIC_EXCEPTION_CONSTRUCTOR_S.newInstance(exceptionMessage);
        } catch (Throwable e) {
            return null;
        }
    }

    public static org.apache.dubbo.rpc.service.GenericException newGenericException(
            String exceptionClass, String exceptionMessage) {
        if (GENERIC_EXCEPTION_CONSTRUCTOR_S_S == null) {
            return null;
        }
        try {
            return GENERIC_EXCEPTION_CONSTRUCTOR_S_S.newInstance(exceptionClass, exceptionMessage);
        } catch (Throwable e) {
            return null;
        }
    }

    public static org.apache.dubbo.rpc.service.GenericException newGenericException(Throwable cause) {
        if (GENERIC_EXCEPTION_CONSTRUCTOR_T == null) {
            return null;
        }
        try {
            return GENERIC_EXCEPTION_CONSTRUCTOR_T.newInstance(cause);
        } catch (Throwable e) {
            return null;
        }
    }

    public static org.apache.dubbo.rpc.service.GenericException newGenericException(
            String message, Throwable cause, String exceptionClass, String exceptionMessage) {
        if (GENERIC_EXCEPTION_CONSTRUCTOR_S_T_S_S == null) {
            return null;
        }
        try {
            return GENERIC_EXCEPTION_CONSTRUCTOR_S_T_S_S.newInstance(message, cause, exceptionClass, exceptionMessage);
        } catch (Throwable e) {
            return null;
        }
    }
}
