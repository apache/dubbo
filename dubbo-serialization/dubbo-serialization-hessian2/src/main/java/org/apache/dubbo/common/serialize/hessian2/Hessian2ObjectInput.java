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

import org.apache.dubbo.common.serialize.Cleanable;
import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import com.alibaba.com.caucho.hessian.io.Hessian2Input;

/**
 * Hessian2 object input implementation
 */
public class Hessian2ObjectInput implements ObjectInput, Cleanable {
    private final Hessian2Input mH2i;
    private final Hessian2FactoryManager hessian2FactoryManager;

    @Deprecated
    public Hessian2ObjectInput(InputStream is) {
        mH2i = new Hessian2Input(is);
        this.hessian2FactoryManager =
                FrameworkModel.defaultModel().getBeanFactory().getOrRegisterBean(Hessian2FactoryManager.class);
        mH2i.setSerializerFactory(hessian2FactoryManager.getSerializerFactory(
                Thread.currentThread().getContextClassLoader()));
    }

    public Hessian2ObjectInput(InputStream is, Hessian2FactoryManager hessian2FactoryManager) {
        mH2i = new Hessian2Input(is);
        this.hessian2FactoryManager = hessian2FactoryManager;
        mH2i.setSerializerFactory(hessian2FactoryManager.getSerializerFactory(
                Thread.currentThread().getContextClassLoader()));
    }

    @Override
    public boolean readBool() throws IOException {
        return mH2i.readBoolean();
    }

    @Override
    public byte readByte() throws IOException {
        return (byte) mH2i.readInt();
    }

    @Override
    public short readShort() throws IOException {
        return (short) mH2i.readInt();
    }

    @Override
    public int readInt() throws IOException {
        return mH2i.readInt();
    }

    @Override
    public long readLong() throws IOException {
        return mH2i.readLong();
    }

    @Override
    public float readFloat() throws IOException {
        return (float) mH2i.readDouble();
    }

    @Override
    public double readDouble() throws IOException {
        return mH2i.readDouble();
    }

    @Override
    public byte[] readBytes() throws IOException {
        return mH2i.readBytes();
    }

    @Override
    public String readUTF() throws IOException {
        return mH2i.readString();
    }

    @Override
    public Object readObject() throws IOException {
        if (!Objects.equals(
                mH2i.getSerializerFactory().getClassLoader(),
                Thread.currentThread().getContextClassLoader())) {
            mH2i.setSerializerFactory(hessian2FactoryManager.getSerializerFactory(
                    Thread.currentThread().getContextClassLoader()));
        }
        return mH2i.readObject();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T readObject(Class<T> cls) throws IOException, ClassNotFoundException {
        if (!Objects.equals(
                mH2i.getSerializerFactory().getClassLoader(),
                Thread.currentThread().getContextClassLoader())) {
            mH2i.setSerializerFactory(hessian2FactoryManager.getSerializerFactory(
                    Thread.currentThread().getContextClassLoader()));
        }
        return (T) mH2i.readObject(cls);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T readObject(Class<T> cls, Type type) throws IOException, ClassNotFoundException {
        if (!Objects.equals(
                mH2i.getSerializerFactory().getClassLoader(),
                Thread.currentThread().getContextClassLoader())) {
            mH2i.setSerializerFactory(hessian2FactoryManager.getSerializerFactory(
                    Thread.currentThread().getContextClassLoader()));
        }
        T result = readObject(cls);
        if (type instanceof ParameterizedType && result != null) {
            result = (T) convertCollectionElementsIfNeeded(result, (ParameterizedType) type);
        }
        return result;
    }

    private Object convertCollectionElementsIfNeeded(Object obj, ParameterizedType type) {
        Type[] typeArgs = type.getActualTypeArguments();

        if (obj instanceof Collection && typeArgs.length >= 1 && typeArgs[0] instanceof Class) {
            Class<?> elementType = (Class<?>) typeArgs[0];
            if (isNarrowNumberType(elementType)) {
                Collection<?> src = (Collection<?>) obj;
                Collection<Object> converted = createCompatibleCollection(src, src.size());
                for (Object e : src) {
                    converted.add(convertNumber(e, elementType));
                }
                return converted;
            }
        }

        if (obj instanceof Map && typeArgs.length >= 2) {
            Class<?> keyType = typeArgs[0] instanceof Class ? (Class<?>) typeArgs[0] : null;
            Class<?> valType = typeArgs[1] instanceof Class ? (Class<?>) typeArgs[1] : null;
            boolean convertKey = keyType != null && isNarrowNumberType(keyType);
            boolean convertVal = valType != null && isNarrowNumberType(valType);
            if (convertKey || convertVal) {
                Map<Object, Object> src = (Map<Object, Object>) obj;
                Map<Object, Object> converted = new java.util.LinkedHashMap<>(src.size());
                for (Map.Entry<Object, Object> entry : src.entrySet()) {
                    Object k = convertKey ? convertNumber(entry.getKey(), keyType) : entry.getKey();
                    Object v = convertVal ? convertNumber(entry.getValue(), valType) : entry.getValue();
                    converted.put(k, v);
                }
                return converted;
            }
        }
        return obj;
    }

    private static boolean isNarrowNumberType(Class<?> type) {
        return type == Byte.class
                || type == byte.class
                || type == Short.class
                || type == short.class
                || type == Float.class
                || type == float.class;
    }

    private static Object convertNumber(Object value, Class<?> targetType) {
        if (!(value instanceof Number)) {
            return value;
        }
        Number num = (Number) value;
        if (targetType == Byte.class || targetType == byte.class) {
            return num.byteValue();
        }
        if (targetType == Short.class || targetType == short.class) {
            return num.shortValue();
        }
        if (targetType == Float.class || targetType == float.class) {
            return num.floatValue();
        }
        return value;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Collection<Object> createCompatibleCollection(Collection<?> source, int size) {
        if (source instanceof LinkedList) {
            return new LinkedList<>();
        }
        if (source instanceof LinkedHashSet) {
            return new LinkedHashSet<>(size);
        }
        if (source instanceof TreeSet) {
            return new TreeSet(((TreeSet) source).comparator());
        }
        if (source instanceof Set) {
            return new HashSet<>(size);
        }
        return new ArrayList<>(size);
    }

    public InputStream readInputStream() throws IOException {
        return mH2i.readInputStream();
    }

    @Override
    public void cleanup() {
        if (mH2i != null) {
            mH2i.reset();
        }
    }
}
