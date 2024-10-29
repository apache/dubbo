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

import java.io.InputStream;
import java.io.Serializable;

import com.alibaba.com.caucho.hessian.io.Deserializer;
import com.alibaba.com.caucho.hessian.io.InputStreamDeserializer;
import com.alibaba.com.caucho.hessian.io.JavaDeserializer;
import com.alibaba.com.caucho.hessian.io.JavaSerializer;
import com.alibaba.com.caucho.hessian.io.RecordDeserializer;
import com.alibaba.com.caucho.hessian.io.RecordUtil;
import com.alibaba.com.caucho.hessian.io.Serializer;
import com.alibaba.com.caucho.hessian.io.SerializerFactory;
import com.alibaba.com.caucho.hessian.io.UnsafeDeserializer;
import com.alibaba.com.caucho.hessian.io.UnsafeSerializer;

public class Hessian2SerializerFactory extends SerializerFactory {

    private final DefaultSerializeClassChecker defaultSerializeClassChecker;

    public Hessian2SerializerFactory(
            ClassLoader classLoader, DefaultSerializeClassChecker defaultSerializeClassChecker) {
        super(classLoader);
        this.defaultSerializeClassChecker = defaultSerializeClassChecker;
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

        if (isEnableUnsafeSerializer() && JavaSerializer.getWriteReplace(cl) == null) {
            return UnsafeSerializer.create(cl);
        } else return JavaSerializer.create(cl);
    }

    @Override
    protected Deserializer getDefaultDeserializer(Class cl) {
        if (InputStream.class.equals(cl)) {
            return InputStreamDeserializer.DESER;
        }

        try {
            // pre-check if class is allow
            defaultSerializeClassChecker.loadClass(getClassLoader(), cl.getName());
        } catch (ClassNotFoundException e) {
            // ignore
        }

        checkSerializable(cl);

        if (RecordUtil.isRecord(cl)) {
            return new RecordDeserializer(cl, getFieldDeserializerFactory());
        } else {
            if (isEnableUnsafeSerializer()) {
                return new UnsafeDeserializer(cl, getFieldDeserializerFactory());
            } else return new JavaDeserializer(cl, getFieldDeserializerFactory());
        }
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
}
