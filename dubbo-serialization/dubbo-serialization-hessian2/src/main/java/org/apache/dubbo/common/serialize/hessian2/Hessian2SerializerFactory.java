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
package org.apache.dubbo.common.serialize.hessian2;

import org.apache.dubbo.common.utils.DefaultSerializeClassChecker;

import java.io.Serializable;

import com.alibaba.com.caucho.hessian.io.AbstractSerializerFactory;
import com.alibaba.com.caucho.hessian.io.Deserializer;
import com.alibaba.com.caucho.hessian.io.HessianProtocolException;
import com.alibaba.com.caucho.hessian.io.JavaDeserializer;
import com.alibaba.com.caucho.hessian.io.JavaSerializer;
import com.alibaba.com.caucho.hessian.io.Serializer;
import com.alibaba.com.caucho.hessian.io.SerializerFactory;

public class Hessian2SerializerFactory extends SerializerFactory {

    private final DefaultSerializeClassChecker defaultSerializeClassChecker;

    public Hessian2SerializerFactory(
            ClassLoader classLoader, DefaultSerializeClassChecker defaultSerializeClassChecker) {
        super(classLoader);
        this.defaultSerializeClassChecker = defaultSerializeClassChecker;
        // hessian-lite 3.2.x resolves classes with a writeReplace() method to
        // JavaSerializer directly and never reaches getDefaultSerializer(), so
        // such classes would otherwise skip checkSerializable(). Prepend a
        // check factory that applies the same check and then falls through to
        // the normal resolution chain. See https://github.com/apache/dubbo/issues/16287.
        addFactory(new WriteReplaceCheckFactory());
    }

    @Override
    public Class<?> loadSerializedClass(String className) throws ClassNotFoundException {
        return defaultSerializeClassChecker.loadClass(getClassLoader(), className);
    }

    @Override
    protected Serializer getDefaultSerializer(Class cl) {
        if (_defaultSerializer != null) return _defaultSerializer;

        try {
            // pre-check if class is allow
            defaultSerializeClassChecker.loadClass(getClassLoader(), cl.getName());
        } catch (ClassNotFoundException e) {
            // ignore
        }

        checkSerializable(cl);

        return new JavaSerializer(cl, getClassLoader());
    }

    @Override
    protected Deserializer getDefaultDeserializer(Class cl) {
        try {
            // pre-check if class is allow
            defaultSerializeClassChecker.loadClass(getClassLoader(), cl.getName());
        } catch (ClassNotFoundException e) {
            // ignore
        }

        checkSerializable(cl);

        return new JavaDeserializer(cl);
    }

    private void checkSerializable(Class<?> cl) {
        // If class is Serializable => ok
        // If class has not implement Serializable
        //      If hessian check serializable => fail
        //      If dubbo class checker check serializable => fail
        //      If both hessian and dubbo class checker allow non-serializable => ok
        if (!Serializable.class.isAssignableFrom(cl)
                && (!isAllowNonSerializable() || defaultSerializeClassChecker.isCheckSerializable())) {
            throw new IllegalStateException(
                    "Serialized class " + cl.getName() + " must implement java.io.Serializable");
        }
    }

    /**
     * Applies {@link #checkSerializable(Class)} to classes carrying a
     * writeReplace() method before hessian-lite resolves them, and returns
     * null so the normal SerializerFactory resolution chain keeps control.
     */
    private final class WriteReplaceCheckFactory extends AbstractSerializerFactory {
        @Override
        public Serializer getSerializer(Class cl) throws HessianProtocolException {
            if (hasWriteReplace(cl)) {
                checkSerializable(cl);
            }
            return null;
        }

        @Override
        public Deserializer getDeserializer(Class cl) throws HessianProtocolException {
            return null;
        }
    }

    /**
     * Mirrors hessian-lite 3.2.x JavaSerializer.getWriteReplace: walks the
     * superclass chain for a no-arg writeReplace() method of any visibility.
     */
    private static boolean hasWriteReplace(Class<?> cl) {
        for (; cl != null; cl = cl.getSuperclass()) {
            for (java.lang.reflect.Method method : cl.getDeclaredMethods()) {
                if ("writeReplace".equals(method.getName()) && method.getParameterTypes().length == 0) {
                    return true;
                }
            }
        }
        return false;
    }
}
