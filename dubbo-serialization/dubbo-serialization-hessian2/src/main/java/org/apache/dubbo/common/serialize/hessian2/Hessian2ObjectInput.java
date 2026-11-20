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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
        if (type instanceof ParameterizedType && containsNarrowableType(type)) {
            // hessian2 encodes Byte/Short/Integer all as int and Float/Double as double on the wire,
            // so the element type of such generic collections can only be restored from the declared
            // generic type. hessian's expectedTypes mechanism is single-level, so nested generics
            // (e.g. List<List<Byte>>, Map<String, List<Byte>>) lose the narrow element type. Read the
            // object with the erased type and then recursively narrow numeric elements to match the
            // declared generic type.
            Object obj = mH2i.readObject(cls);
            return (T) narrowByType(obj, type);
        }
        return (T) mH2i.readObject(cls);
    }

    /**
     * Checks whether the given {@link Type} declares any narrow primitive-wrapper element type
     * (Byte/Short/Float/Character) anywhere in the generic hierarchy. Only such types need the
     * recursive narrowing pass, avoiding unnecessary copies for e.g. {@code List<String>}.
     */
    private boolean containsNarrowableType(Type type) {
        if (type instanceof ParameterizedType) {
            Type[] typeArgs = ((ParameterizedType) type).getActualTypeArguments();
            for (Type typeArg : typeArgs) {
                if (containsNarrowableType(typeArg)) {
                    return true;
                }
            }
            return false;
        }
        return type == Byte.class || type == Short.class || type == Float.class || type == Character.class;
    }

    /**
     * Recursively narrows widened numeric elements (Integer/Double) inside generic collections to the
     * element types declared by {@code type}. Returns the input object unchanged when no element needs
     * to be narrowed.
     */
    @SuppressWarnings("unchecked")
    private Object narrowByType(Object obj, Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type rawType = parameterizedType.getRawType();
            Type[] typeArgs = parameterizedType.getActualTypeArguments();
            if (rawType instanceof Class
                    && Collection.class.isAssignableFrom((Class<?>) rawType)
                    && typeArgs.length == 1) {
                Type elementType = typeArgs[0];
                if (obj instanceof List) {
                    List<Object> result = new ArrayList<>(((List<?>) obj).size());
                    for (Object element : (List<?>) obj) {
                        result.add(narrowByType(element, elementType));
                    }
                    return result;
                }
                if (obj instanceof Set) {
                    Set<Object> result = new LinkedHashSet<>();
                    for (Object element : (Set<?>) obj) {
                        result.add(narrowByType(element, elementType));
                    }
                    return result;
                }
                if (obj instanceof Collection) {
                    Collection<Object> result = new ArrayList<>();
                    for (Object element : (Collection<?>) obj) {
                        result.add(narrowByType(element, elementType));
                    }
                    return result;
                }
            } else if (rawType instanceof Class
                    && Map.class.isAssignableFrom((Class<?>) rawType)
                    && typeArgs.length == 2) {
                Type keyType = typeArgs[0];
                Type valueType = typeArgs[1];
                if (obj instanceof Map) {
                    Map<Object, Object> result = new LinkedHashMap<>();
                    for (Map.Entry<?, ?> entry : ((Map<?, ?>) obj).entrySet()) {
                        result.put(narrowByType(entry.getKey(), keyType), narrowByType(entry.getValue(), valueType));
                    }
                    return result;
                }
            }
        } else if (type instanceof Class) {
            Class<?> clazz = (Class<?>) type;
            if (clazz == Byte.class && obj instanceof Integer) {
                return Byte.valueOf(((Number) obj).byteValue());
            }
            if (clazz == Short.class && obj instanceof Integer) {
                return Short.valueOf(((Number) obj).shortValue());
            }
            if (clazz == Float.class && obj instanceof Double) {
                return Float.valueOf(((Number) obj).floatValue());
            }
            if (clazz == Character.class && obj instanceof Integer) {
                return (char) ((Number) obj).intValue();
            }
        }
        return obj;
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
