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
package com.alibaba.dubbo.common.serialize.support.dubbo;

import com.alibaba.dubbo.common.bytecode.ClassGenerator;
import com.alibaba.dubbo.common.io.UnsafeByteArrayInputStream;
import com.alibaba.dubbo.common.io.UnsafeByteArrayOutputStream;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.serialize.support.java.CompactedObjectInputStream;
import com.alibaba.dubbo.common.serialize.support.java.CompactedObjectOutputStream;
import com.alibaba.dubbo.common.utils.ClassHelper;
import com.alibaba.dubbo.common.utils.IOUtils;
import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.alibaba.dubbo.common.utils.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;

/**
 * Builder.
 *
 * @param <T> type.
 */

/**
 * 对象序列化代码构建器
 *
 * @param <T> 泛型
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class Builder<T> implements GenericDataFlags {

    /**
     * 通用 Serializable 的 Builder 对象，使用 Java 原生序列化方式实现。
     *
     * 适用于例如 Throwable 对象，或者带有 transient 修饰符属性的 Serializable 实现类
     */
    static final Builder<Serializable> SerializableBuilder = new Builder<Serializable>() {

        @Override
        public Class<Serializable> getType() {
            return Serializable.class;
        }

        @Override
        public void writeTo(Serializable obj, GenericObjectOutput out) throws IOException {
            // NULL ，写入 OBJECT_NULL 到 mBuffer 中
            if (obj == null) {
                out.write0(OBJECT_NULL);
            // 非空
            } else {
                // 写入 OBJECT_STREAM 到 mBuffer 中
                out.write0(OBJECT_STREAM);
                // 使用 compactjava 序列化实现，进行序列化
                UnsafeByteArrayOutputStream bos = new UnsafeByteArrayOutputStream();
                CompactedObjectOutputStream oos = new CompactedObjectOutputStream(bos);
                oos.writeObject(obj);
                oos.flush();
                bos.close();
                byte[] b = bos.toByteArray();
                // 写入 Length( 字节数组长度 ) 到 mBuffer 中
                out.writeUInt(b.length);
                // 写入 字节数组 到 mBuffer 中
                out.write0(b, 0, b.length);
            }
        }

        @Override
        public Serializable parseFrom(GenericObjectInput in) throws IOException {
            // 读取首位字节
            byte b = in.read0();
            // NULL ，返回 null
            if (b == OBJECT_NULL) {
                return null;
            }
            if (b != OBJECT_STREAM) {
                throw new IOException("Input format error, expect OBJECT_NULL|OBJECT_STREAM, get " + b + ".");
            }

            // 使用 compactjava 序列化实现，进行反序列化
            UnsafeByteArrayInputStream bis = new UnsafeByteArrayInputStream(in.read0(in.readUInt()));
            CompactedObjectInputStream ois = new CompactedObjectInputStream(bis);
            try {
                return (Serializable) ois.readObject();
            } catch (ClassNotFoundException e) {
                throw new IOException(StringUtils.toString(e));
            }
        }

    };

    private static final AtomicLong BUILDER_CLASS_COUNTER = new AtomicLong(0);

    private static final String BUILDER_CLASS_NAME = Builder.class.getName();

    /**
     * 实现 Serializable 接口的类的 Builder 对象缓存
     */
    private static final Map<Class<?>, Builder<?>> BuilderMap = new ConcurrentHashMap<Class<?>, Builder<?>>();
    /**
     * 未实现 Serializable 接口的类的 Builder 对象缓存
     */
    private static final Map<Class<?>, Builder<?>> nonSerializableBuilderMap = new ConcurrentHashMap<Class<?>, Builder<?>>();

    private static final String FIELD_CONFIG_SUFFIX = ".fc";

    private static final int MAX_FIELD_CONFIG_FILE_SIZE = 16 * 1024;

    /**
     * Field 名的排序
     */
    private static final Comparator<String> FNC = new Comparator<String>() {

        @Override
        public int compare(String n1, String n2) {
            return compareFieldName(n1, n2);
        }

    };

    /**
     * Field 的排序，和 {@link #FNC} 是一致的
     */
    private static final Comparator<Field> FC = new Comparator<Field>() {

        @Override
        public int compare(Field f1, Field f2) {
            return compareFieldName(f1.getName(), f2.getName());
        }

    };

    /**
     * 构造方法的排序
     */
    private static final Comparator<Constructor> CC = new Comparator<Constructor>() {
        public int compare(Constructor o1, Constructor o2) {
            return o1.getParameterTypes().length - o2.getParameterTypes().length;
        }
    };

    // class-descriptor mapper
    /**
     * 类描述数组
     */
    private static final List<String> mDescList = new ArrayList<String>();
    /**
     * 类描述映射
     */
    private static final Map<String, Integer> mDescMap = new ConcurrentHashMap<String, Integer>();
    /**
     * ClassDescriptorMapper 默认实现类
     */
    public static ClassDescriptorMapper DEFAULT_CLASS_DESCRIPTOR_MAPPER = new ClassDescriptorMapper() {

        @Override
        public String getDescriptor(int index) {
            if (index < 0 || index >= mDescList.size()) {
                return null;
            }
            return mDescList.get(index);
        }

        @Override
        public int getDescriptorIndex(String desc) {
            Integer ret = mDescMap.get(desc);
            return ret == null ? -1 : ret;
        }

    };

    // Must be protected. by qian.lei
    protected static Logger logger = LoggerFactory.getLogger(Builder.class);

    /**
     * 通用数组( Array ) 的 Builder 对象
     */
    static final Builder<Object[]> GenericArrayBuilder = new AbstractObjectBuilder<Object[]>() {

        @Override
        public Class<Object[]> getType() {
            return Object[].class;
        }

        @Override
        protected Object[] newInstance(GenericObjectInput in) throws IOException {
            // 读取数组长度，并创建数组对象
            return new Object[in.readUInt()];
        }

        @Override
        protected void readObject(Object[] ret, GenericObjectInput in) throws IOException {
            // 循环读取每个对象到 ret 中
            for (int i = 0; i < ret.length; i++) {
                ret[i] = in.readObject();
            }
        }

        @Override
        protected void writeObject(Object[] obj, GenericObjectOutput out) throws IOException {
            // 写入 Length( 数组大小 ) 到 mBuffer
            out.writeUInt(obj.length);
            // 循环写入每个对象到 mBuffer 中
            for (Object item : obj) {
                out.writeObject(item);
            }
        }

    };

    /**
     * 通用 Object 的 Builder 对象
     */
    static final Builder<Object> GenericBuilder = new Builder<Object>() {

        @Override
        public Class<Object> getType() {
            return Object.class;
        }

        @Override
        public void writeTo(Object obj, GenericObjectOutput out) throws IOException {
            out.writeObject(obj);
        }

        @Override
        public Object parseFrom(GenericObjectInput in) throws IOException {
            return in.readObject();
        }

    };

    static {
        addDesc(boolean[].class);
        addDesc(byte[].class);
        addDesc(char[].class);
        addDesc(short[].class);
        addDesc(int[].class);
        addDesc(long[].class);
        addDesc(float[].class);
        addDesc(double[].class);

        addDesc(Boolean.class);
        addDesc(Byte.class);
        addDesc(Character.class);
        addDesc(Short.class);
        addDesc(Integer.class);
        addDesc(Long.class);
        addDesc(Float.class);
        addDesc(Double.class);

        addDesc(String.class);
        addDesc(String[].class);

        addDesc(ArrayList.class);
        addDesc(HashMap.class);
        addDesc(HashSet.class);
        addDesc(Date.class);
        addDesc(java.sql.Date.class);
        addDesc(java.sql.Time.class);
        addDesc(java.sql.Timestamp.class);
        addDesc(java.util.LinkedList.class);
        addDesc(java.util.LinkedHashMap.class);
        addDesc(java.util.LinkedHashSet.class);

        register(byte[].class, new Builder<byte[]>() {
            @Override
            public Class<byte[]> getType() {
                return byte[].class;
            }

            @Override
            public void writeTo(byte[] obj, GenericObjectOutput out) throws IOException {
                out.writeBytes(obj);
            }

            @Override
            public byte[] parseFrom(GenericObjectInput in) throws IOException {
                return in.readBytes();
            }
        });
        register(Boolean.class, new Builder<Boolean>() {
            @Override
            public Class<Boolean> getType() {
                return Boolean.class;
            }

            @Override
            public void writeTo(Boolean obj, GenericObjectOutput out) throws IOException {
                if (obj == null)
                    out.write0(VARINT_N1);
                else if (obj.booleanValue())
                    out.write0(VARINT_1);
                else
                    out.write0(VARINT_0);
            }

            @Override
            public Boolean parseFrom(GenericObjectInput in) throws IOException {
                byte b = in.read0();
                switch (b) {
                    case VARINT_N1:
                        return null;
                    case VARINT_0:
                        return Boolean.FALSE;
                    case VARINT_1:
                        return Boolean.TRUE;
                    default:
                        throw new IOException("Input format error, expect VARINT_N1|VARINT_0|VARINT_1, get " + b + ".");
                }
            }
        });
        register(Byte.class, new Builder<Byte>() {
            @Override
            public Class<Byte> getType() {
                return Byte.class;
            }

            @Override
            public void writeTo(Byte obj, GenericObjectOutput out) throws IOException {
                if (obj == null) {
                    out.write0(OBJECT_NULL);
                } else {
                    out.write0(OBJECT_VALUE);
                    out.writeByte(obj.byteValue());
                }
            }

            @Override
            public Byte parseFrom(GenericObjectInput in) throws IOException {
                byte b = in.read0();
                if (b == OBJECT_NULL)
                    return null;
                if (b != OBJECT_VALUE)
                    throw new IOException("Input format error, expect OBJECT_NULL|OBJECT_VALUE, get " + b + ".");

                return Byte.valueOf(in.readByte());
            }
        });
        register(Character.class, new Builder<Character>() {
            @Override
            public Class<Character> getType() {
                return Character.class;
            }

            @Override
            public void writeTo(Character obj, GenericObjectOutput out) throws IOException {
                if (obj == null) {
                    out.write0(OBJECT_NULL);
                } else {
                    out.write0(OBJECT_VALUE);
                    out.writeShort((short) obj.charValue());
                }
            }

            @Override
            public Character parseFrom(GenericObjectInput in) throws IOException {
                byte b = in.read0();
                if (b == OBJECT_NULL)
                    return null;
                if (b != OBJECT_VALUE)
                    throw new IOException("Input format error, expect OBJECT_NULL|OBJECT_VALUE, get " + b + ".");

                return Character.valueOf((char) in.readShort());
            }
        });
        register(Short.class, new Builder<Short>() {
            @Override
            public Class<Short> getType() {
                return Short.class;
            }

            @Override
            public void writeTo(Short obj, GenericObjectOutput out) throws IOException {
                if (obj == null) {
                    out.write0(OBJECT_NULL);
                } else {
                    out.write0(OBJECT_VALUE);
                    out.writeShort(obj.shortValue());
                }
            }

            @Override
            public Short parseFrom(GenericObjectInput in) throws IOException {
                byte b = in.read0();
                if (b == OBJECT_NULL)
                    return null;
                if (b != OBJECT_VALUE)
                    throw new IOException("Input format error, expect OBJECT_NULL|OBJECT_VALUE, get " + b + ".");

                return Short.valueOf(in.readShort());
            }
        });
        register(Integer.class, new Builder<Integer>() {
            @Override
            public Class<Integer> getType() {
                return Integer.class;
            }

            @Override
            public void writeTo(Integer obj, GenericObjectOutput out) throws IOException {
                if (obj == null) {
                    out.write0(OBJECT_NULL);
                } else {
                    out.write0(OBJECT_VALUE);
                    out.writeInt(obj.intValue());
                }
            }

            @Override
            public Integer parseFrom(GenericObjectInput in) throws IOException {
                byte b = in.read0();
                if (b == OBJECT_NULL)
                    return null;
                if (b != OBJECT_VALUE)
                    throw new IOException("Input format error, expect OBJECT_NULL|OBJECT_VALUE, get " + b + ".");

                return Integer.valueOf(in.readInt());
            }
        });
        register(Long.class, new Builder<Long>() {
            @Override
            public Class<Long> getType() {
                return Long.class;
            }

            @Override
            public void writeTo(Long obj, GenericObjectOutput out) throws IOException {
                if (obj == null) {
                    out.write0(OBJECT_NULL);
                } else {
                    out.write0(OBJECT_VALUE);
                    out.writeLong(obj.longValue());
                }
            }

            @Override
            public Long parseFrom(GenericObjectInput in) throws IOException {
                byte b = in.read0();
                if (b == OBJECT_NULL)
                    return null;
                if (b != OBJECT_VALUE)
                    throw new IOException("Input format error, expect OBJECT_NULL|OBJECT_VALUE, get " + b + ".");

                return Long.valueOf(in.readLong());
            }
        });
        register(Float.class, new Builder<Float>() {
            @Override
            public Class<Float> getType() {
                return Float.class;
            }

            @Override
            public void writeTo(Float obj, GenericObjectOutput out) throws IOException {
                if (obj == null) {
                    out.write0(OBJECT_NULL);
                } else {
                    out.write0(OBJECT_VALUE);
                    out.writeFloat(obj.floatValue());
                }
            }

            @Override
            public Float parseFrom(GenericObjectInput in) throws IOException {
                byte b = in.read0();
                if (b == OBJECT_NULL)
                    return null;
                if (b != OBJECT_VALUE)
                    throw new IOException("Input format error, expect OBJECT_NULL|OBJECT_VALUE, get " + b + ".");

                return new Float(in.readFloat());
            }
        });
        register(Double.class, new Builder<Double>() {
            @Override
            public Class<Double> getType() {
                return Double.class;
            }

            @Override
            public void writeTo(Double obj, GenericObjectOutput out) throws IOException {
                if (obj == null) {
                    out.write0(OBJECT_NULL);
                } else {
                    out.write0(OBJECT_VALUE);
                    out.writeDouble(obj.doubleValue());
                }
            }

            @Override
            public Double parseFrom(GenericObjectInput in) throws IOException {
                byte b = in.read0();
                if (b == OBJECT_NULL)
                    return null;
                if (b != OBJECT_VALUE)
                    throw new IOException("Input format error, expect OBJECT_NULL|OBJECT_VALUE, get " + b + ".");

                return new Double(in.readDouble());
            }
        });
        register(String.class, new Builder<String>() {
            @Override
            public Class<String> getType() {
                return String.class;
            }

            @Override
            public String parseFrom(GenericObjectInput in) throws IOException {
                return in.readUTF();
            }

            @Override
            public void writeTo(String obj, GenericObjectOutput out) throws IOException {
                out.writeUTF(obj);
            }
        });
        register(StringBuilder.class, new Builder<StringBuilder>() {
            @Override
            public Class<StringBuilder> getType() {
                return StringBuilder.class;
            }

            @Override
            public StringBuilder parseFrom(GenericObjectInput in) throws IOException {
                return new StringBuilder(in.readUTF());
            }

            @Override
            public void writeTo(StringBuilder obj, GenericObjectOutput out) throws IOException {
                out.writeUTF(obj.toString());
            }
        });
        register(StringBuffer.class, new Builder<StringBuffer>() {
            @Override
            public Class<StringBuffer> getType() {
                return StringBuffer.class;
            }

            @Override
            public StringBuffer parseFrom(GenericObjectInput in) throws IOException {
                return new StringBuffer(in.readUTF());
            }

            @Override
            public void writeTo(StringBuffer obj, GenericObjectOutput out) throws IOException {
                out.writeUTF(obj.toString());
            }
        });

        // java.util
        register(ArrayList.class, new Builder<ArrayList>() {
            @Override
            public Class<ArrayList> getType() {
                return ArrayList.class;
            }

            @Override
            public void writeTo(ArrayList obj, GenericObjectOutput out) throws IOException {
                if (obj == null) {
                    out.write0(OBJECT_NULL);
                } else {
                    out.write0(OBJECT_VALUES);
                    out.writeUInt(obj.size());
                    for (Object item : obj)
                        out.writeObject(item);
                }
            }

            @Override
            public ArrayList parseFrom(GenericObjectInput in) throws IOException {
                byte b = in.read0();
                if (b == OBJECT_NULL)
                    return null;
                if (b != OBJECT_VALUES)
                    throw new IOException("Input format error, expect OBJECT_NULL|OBJECT_VALUES, get " + b + ".");

                int len = in.readUInt();
                ArrayList ret = new ArrayList(len);
                for (int i = 0; i < len; i++)
                    ret.add(in.readObject());
                return ret;
            }
        });
        register(HashMap.class, new Builder<HashMap>() {

            @Override
            public Class<HashMap> getType() {
                return HashMap.class;
            }

            @Override
            public void writeTo(HashMap obj, GenericObjectOutput out) throws IOException {
                // NULL ，写入 OBJECT_NULL 到 mBuffer 中
                if (obj == null) {
                    out.write0(OBJECT_NULL);
                // HashMap 非空
                } else {
                    // 写入 OBJECT_MAP 到 mBuffer 中
                    out.write0(OBJECT_MAP);
                    // 写入 Length(Map 大小) 到 mBuffer 中
                    out.writeUInt(obj.size());
                    // 写入 KV 到 mBuffer 中
                    for (Map.Entry entry : (Set<Map.Entry>) obj.entrySet()) {
                        out.writeObject(entry.getKey());
                        out.writeObject(entry.getValue());
                    }
                }
            }

            @Override
            public HashMap parseFrom(GenericObjectInput in) throws IOException {
                // 读取首位字节
                byte b = in.read0();
                // NULL ，返回 null
                if (b == OBJECT_NULL) {
                    return null;
                }
                if (b != OBJECT_MAP) {
                    throw new IOException("Input format error, expect OBJECT_NULL|OBJECT_MAP, get " + b + ".");
                }

                // 读取 Length(Map 大小)
                int len = in.readUInt();
                // 循环读取 KV 到 HashMap
                HashMap ret = new HashMap(len);
                for (int i = 0; i < len; i++) {
                    ret.put(in.readObject(), in.readObject());
                }
                return ret;
            }

        });
        register(HashSet.class, new Builder<HashSet>() {
            @Override
            public Class<HashSet> getType() {
                return HashSet.class;
            }

            @Override
            public void writeTo(HashSet obj, GenericObjectOutput out) throws IOException {
                if (obj == null) {
                    out.write0(OBJECT_NULL);
                } else {
                    out.write0(OBJECT_VALUES);
                    out.writeUInt(obj.size());
                    for (Object item : obj)
                        out.writeObject(item);
                }
            }

            @Override
            public HashSet parseFrom(GenericObjectInput in) throws IOException {
                byte b = in.read0();
                if (b == OBJECT_NULL)
                    return null;
                if (b != OBJECT_VALUES)
                    throw new IOException("Input format error, expect OBJECT_NULL|OBJECT_VALUES, get " + b + ".");

                int len = in.readUInt();
                HashSet ret = new HashSet(len);
                for (int i = 0; i < len; i++)
                    ret.add(in.readObject());
                return ret;
            }
        });

        register(Date.class, new Builder<Date>() {
            @Override
            public Class<Date> getType() {
                return Date.class;
            }

            @Override
            public void writeTo(Date obj, GenericObjectOutput out) throws IOException {
                if (obj == null) {
                    out.write0(OBJECT_NULL);
                } else {
                    out.write0(OBJECT_VALUE);
                    out.writeLong(obj.getTime());
                }
            }

            @Override
            public Date parseFrom(GenericObjectInput in) throws IOException {
                byte b = in.read0();
                if (b == OBJECT_NULL)
                    return null;
                if (b != OBJECT_VALUE)
                    throw new IOException("Input format error, expect OBJECT_NULL|OBJECT_VALUE, get " + b + ".");

                return new Date(in.readLong());
            }
        });

        // java.sql
        register(java.sql.Date.class, new Builder<java.sql.Date>() {
            @Override
            public Class<java.sql.Date> getType() {
                return java.sql.Date.class;
            }

            @Override
            public void writeTo(java.sql.Date obj, GenericObjectOutput out) throws IOException {
                if (obj == null) {
                    out.write0(OBJECT_NULL);
                } else {
                    out.write0(OBJECT_VALUE);
                    out.writeLong(obj.getTime());
                }
            }

            @Override
            public java.sql.Date parseFrom(GenericObjectInput in) throws IOException {
                byte b = in.read0();
                if (b == OBJECT_NULL)
                    return null;
                if (b != OBJECT_VALUE)
                    throw new IOException("Input format error, expect OBJECT_NULL|OBJECT_VALUE, get " + b + ".");

                return new java.sql.Date(in.readLong());
            }
        });
        register(java.sql.Timestamp.class, new Builder<java.sql.Timestamp>() {
            @Override
            public Class<java.sql.Timestamp> getType() {
                return java.sql.Timestamp.class;
            }

            @Override
            public void writeTo(java.sql.Timestamp obj, GenericObjectOutput out) throws IOException {
                if (obj == null) {
                    out.write0(OBJECT_NULL);
                } else {
                    out.write0(OBJECT_VALUE);
                    out.writeLong(obj.getTime());
                }
            }

            @Override
            public java.sql.Timestamp parseFrom(GenericObjectInput in) throws IOException {
                byte b = in.read0();
                if (b == OBJECT_NULL)
                    return null;
                if (b != OBJECT_VALUE)
                    throw new IOException("Input format error, expect OBJECT_NULL|OBJECT_VALUE, get " + b + ".");

                return new java.sql.Timestamp(in.readLong());
            }
        });
        register(java.sql.Time.class, new Builder<java.sql.Time>() {
            @Override
            public Class<java.sql.Time> getType() {
                return java.sql.Time.class;
            }

            @Override
            public void writeTo(java.sql.Time obj, GenericObjectOutput out) throws IOException {
                if (obj == null) {
                    out.write0(OBJECT_NULL);
                } else {
                    out.write0(OBJECT_VALUE);
                    out.writeLong(obj.getTime());
                }
            }

            @Override
            public java.sql.Time parseFrom(GenericObjectInput in) throws IOException {
                byte b = in.read0();
                if (b == OBJECT_NULL)
                    return null;
                if (b != OBJECT_VALUE)
                    throw new IOException("Input format error, expect OBJECT_NULL|OBJECT_VALUE, get " + b + ".");

                return new java.sql.Time(in.readLong());
            }
        });
    }

    protected Builder() {
    }

    /**
     * 获得类对应的 Builder 对象
     *
     * @param c 类
     * @param isAllowNonSerializable 对象是否允许不实现 {@link java.io.Serializable} 接口
     * @param <T> 泛型
     * @return Builder 对象
     */
    public static <T> Builder<T> register(Class<T> c, boolean isAllowNonSerializable) {
        // Object 类，或者接口，使用 GenericBuilder
        if (c == Object.class || c.isInterface()) {
            return (Builder<T>) GenericBuilder;
        }
        // Array 类型，使用 GenericArrayBuilder
        if (c == Object[].class) {
            return (Builder<T>) GenericArrayBuilder;
        }

        // 获得 Builder 对象
        Builder<T> b = (Builder<T>) BuilderMap.get(c);
        if (null != b) {
            return b;
        }

        // 要求实现 Serializable 接口，但是并未实现，则抛出 IllegalStateException 异常
        boolean isSerializable = Serializable.class.isAssignableFrom(c);
        if (!isAllowNonSerializable && !isSerializable) {
            throw new IllegalStateException("Serialized class " + c.getName() +
                    " must implement java.io.Serializable (dubbo codec setting: isAllowNonSerializable = false)");
        }

        // 获得 Builder 对象
        b = (Builder<T>) nonSerializableBuilderMap.get(c);
        if (null != b) {
            return b;
        }

        // 不存在，使用 Javassist 生成对应的 Builder 类，并进行创建
        b = newBuilder(c);

        // 添加到 Builder 对象缓存中
        if (isSerializable) {
            BuilderMap.put(c, b);
        } else {
            nonSerializableBuilderMap.put(c, b);
        }

        return b;
    }

    public static <T> Builder<T> register(Class<T> c) {
        return register(c, false);
    }

    public static <T> void register(Class<T> c, Builder<T> b) {
        if (Serializable.class.isAssignableFrom(c)) {
            BuilderMap.put(c, b);
        } else {
            nonSerializableBuilderMap.put(c, b);
        }
    }

    private static <T> Builder<T> newBuilder(Class<T> c) {
        // 基础类型，已经内置相应的 Builder 实现类，抛出 RuntimeException 异常。因为，已经在 GenericDataInput 和 GenericDataOutput 实现。
        if (c.isPrimitive()) {
            throw new RuntimeException("Can not create builder for primitive type: " + c);
        }

        if (logger.isInfoEnabled())
            logger.info("create Builder for class: " + c);

        Builder<?> builder;
        // 创建 Array Builder 对象
        if (c.isArray()) {
            builder = newArrayBuilder(c);
        // 创建 Object Builder 对象
        } else {
            builder = newObjectBuilder(c);
        }
        return (Builder<T>) builder;
    }

    private static Builder<?> newArrayBuilder(Class<?> c) {
        // 如果是接口，使用 GenericArrayBuilder
        Class<?> cc = c.getComponentType();
        if (cc.isInterface()) {
            return GenericArrayBuilder;
        }

        ClassLoader cl = ClassHelper.getCallerClassLoader(Builder.class);

        // 完整类名
        String cn = ReflectUtils.getName(c), ccn = ReflectUtils.getName(cc); // get class name as int[][], double[].
        // 对应的 Builder 类名
        String bcn = BUILDER_CLASS_NAME + "$bc" + BUILDER_CLASS_COUNTER.getAndIncrement();

        // 第一层数组。
        int ix = cn.indexOf(']');
        String s1 = cn.substring(0, ix), s2 = cn.substring(ix); // if name='int[][]' then s1='int[', s2='][]'

        // `#writeObject(T obj, GenericObjectOutput out)` 抽象方法的实现代码字符串。目前这段是方法头
        StringBuilder cwt = new StringBuilder("public void writeTo(Object obj, ").append(GenericObjectOutput.class.getName()).append(" out) throws java.io.IOException{"); // writeTo code.
        // `#readObject(T ret, GenericObjectInput in)` 抽象方法的实现代码字符串。目前这段是方法头
        StringBuilder cpf = new StringBuilder("public Object parseFrom(").append(GenericObjectInput.class.getName()).append(" in) throws java.io.IOException{"); // parseFrom code.

        // `#writeObject(T obj, GenericObjectOutput out)` 抽象方法的 Length + 循环写入 的代码字符串
        cwt.append("if( $1 == null ){ $2.write0(OBJECT_NULL); return; }");
        cwt.append(cn).append(" v = (").append(cn).append(")$1; int len = v.length; $2.write0(OBJECT_VALUES); $2.writeUInt(len); for(int i=0;i<len;i++){ ");

        // `#readObject(T obj, GenericObjectOutput out)` 抽象方法的 Length + 循环读取 的代码字符串
        cpf.append("byte b = $1.read0(); if( b == OBJECT_NULL ) return null; if( b != OBJECT_VALUES ) throw new java.io.IOException(\"Input format error, expect OBJECT_NULL|OBJECT_VALUES, get \" + b + \".\");");
        cpf.append("int len = $1.readUInt(); if( len == 0 ) return new ").append(s1).append('0').append(s2).append("; ");
        cpf.append(cn).append(" ret = new ").append(s1).append("len").append(s2).append("; for(int i=0;i<len;i++){ ");

        Builder<?> builder = null;
        // 基本类型，直接 GenericDataOutput 和 GenericDataInput 对应的基础属性的序列化方法。
        if (cc.isPrimitive()) {
            // 添加该属性的在 `#writeObject(T obj, GenericObjectOutput out)` 和 `#readObject(T ret, GenericObjectInput in)` 中的序列化和反序列化的代码
            if (cc == boolean.class) {
                cwt.append("$2.writeBool(v[i]);");
                cpf.append("ret[i] = $1.readBool();");
            } else if (cc == byte.class) {
                cwt.append("$2.writeByte(v[i]);");
                cpf.append("ret[i] = $1.readByte();");
            } else if (cc == char.class) {
                cwt.append("$2.writeShort((short)v[i]);");
                cpf.append("ret[i] = (char)$1.readShort();");
            } else if (cc == short.class) {
                cwt.append("$2.writeShort(v[i]);");
                cpf.append("ret[i] = $1.readShort();");
            } else if (cc == int.class) {
                cwt.append("$2.writeInt(v[i]);");
                cpf.append("ret[i] = $1.readInt();");
            } else if (cc == long.class) {
                cwt.append("$2.writeLong(v[i]);");
                cpf.append("ret[i] = $1.readLong();");
            } else if (cc == float.class) {
                cwt.append("$2.writeFloat(v[i]);");
                cpf.append("ret[i] = $1.readFloat();");
            } else if (cc == double.class) {
                cwt.append("$2.writeDouble(v[i]);");
                cpf.append("ret[i] = $1.readDouble();");
            }
        // 对象，使用对象对应的 Builder 对象
        // 如果是多层数组，会通过 Builder 嵌套的方式，实现一层一层的序列化，类似嵌套对象。
        } else {
            // 获得对象对应的 Builder 对象
            builder = register(cc);
            // 添加该属性的在 `#writeObject(T obj, GenericObjectOutput out)` 和 `#readObject(T ret, GenericObjectInput in)` 中的序列化和反序列化的代码
            cwt.append("builder.writeTo(v[i], $2);");
            cpf.append("ret[i] = (").append(ccn).append(")builder.parseFrom($1);");
        }
        // 方法尾部的 `}`
        cwt.append(" } }");
        cpf.append(" } return ret; }");

        // 创建 Builder 代码生成器
        ClassGenerator cg = ClassGenerator.newInstance(cl);
        // 设置类名
        cg.setClassName(bcn);
        // 设置父类为 Builder.class
        cg.setSuperClass(Builder.class);
        // 添加默认构造方法
        cg.addDefaultConstructor();
        // 添加 `builders` 静态属性
        if (builder != null) {
            cg.addField("public static " + BUILDER_CLASS_NAME + " builder;");
        }
        // 添加 `#getType()` 的实现代码字符串
        cg.addMethod("public Class getType(){ return " + cn + ".class; }");
        // 添加 `#writeObject(T obj, GenericObjectOutput out)` 的实现代码字符串
        cg.addMethod(cwt.toString());
        // 添加 `#readObject(T obj, GenericObjectOutput out)` 的实现代码字符串
        cg.addMethod(cpf.toString());
        try {
            // 生成类
            Class<?> wc = cg.toClass();
            // set static field.
            // 设置构造方法，若需要反射创建对象
            if (builder != null) {
                wc.getField("builder").set(null, builder);
            }
            // 创建 Builder 对象
            return (Builder<?>) wc.newInstance();
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e.getMessage());
        } finally {
            cg.release();
        }
    }

    private static Builder<?> newObjectBuilder(final Class<?> c) {
        // 枚举类，创建 Enum Builder 对象
        if (c.isEnum()) {
            return newEnumBuilder(c);
        }

        // 不支持匿名类，抛出 RuntimeException 异常
        if (c.isAnonymousClass()) {
            throw new RuntimeException("Can not instantiation anonymous class: " + c);
        }

        // 不支持非静态的内部类，抛出 RuntimeException 异常
        if (c.getEnclosingClass() != null && !Modifier.isStatic(c.getModifiers())) {
            throw new RuntimeException("Can not instantiation inner and non-static class: " + c);
        }

        // Throwable 类，使用内置的 Serialize Builder 对象
        if (Throwable.class.isAssignableFrom(c)) {
            return SerializableBuilder;
        }

        ClassLoader cl = ClassHelper.getCallerClassLoader(Builder.class);

        // is same package.
        boolean isp; // 是否在相同包
        String cn = c.getName(), // 完整类名
               bcn; // 对应的 Builder 类名
        if (c.getClassLoader() == null) { // 不存在类加载器，和 Builder 类相同包
            // is system class. if( cn.startsWith("java.") || cn.startsWith("javax.") || cn.startsWith("sun.") )
            isp = false;
            bcn = BUILDER_CLASS_NAME + "$bc" + BUILDER_CLASS_COUNTER.getAndIncrement();
        } else { // 存在类加载器，和 c 类相同包
            isp = true;
            bcn = cn + "$bc" + BUILDER_CLASS_COUNTER.getAndIncrement();
        }

        // is Collection, is Map, is Serializable.
        boolean isc = Collection.class.isAssignableFrom(c); // 是否 Collection
        boolean ism = !isc && Map.class.isAssignableFrom(c); // 是否 Map
        boolean iss = !(isc || ism) && Serializable.class.isAssignableFrom(c); // 是否 Serializable

        // 从类对应的 `.fc` 后缀配置文件，读取属性的顺序。
        // 对应例子为 SimpleDO.java 和 SimpleDO.fc
        // deal with fields.
        String[] fns = null; // fix-order fields names
        InputStream is = c.getResourceAsStream(c.getSimpleName() + FIELD_CONFIG_SUFFIX); // load field-config file.
        if (is != null) {
            try {
                int len = is.available();
                if (len > 0) {
                    if (len > MAX_FIELD_CONFIG_FILE_SIZE) {
                        throw new RuntimeException("Load [" + c.getName() + "] field-config file error: File-size too larger");
                    }
                    // 逐行读取
                    String[] lines = IOUtils.readLines(is);
                    if (lines.length > 0) {
                        List<String> list = new ArrayList<String>();
                        // 排序
                        for (String line : lines) {
                            fns = line.split(",");
                            Arrays.sort(fns, FNC);
                            list.addAll(Arrays.asList(fns));
                        }
                        fns = list.toArray(new String[0]);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Load [" + c.getName() + "] field-config file error: " + e.getMessage());
            } finally {
                try {
                    is.close();
                } catch (IOException ignored) {
                }
            }
        }

        Field f, fs[];
        if (fns != null) {
            // 按照 `.fc` 后缀的配置文件，进行排序
            fs = new Field[fns.length];
            for (int i = 0; i < fns.length; i++) {
                String fn = fns[i];
                try {
                    // 获得对应的属性
                    f = c.getDeclaredField(fn);
                    // 判断是否符合忽略属性的条件，若是，抛出 RuntimeException 异常
                    int mod = f.getModifiers();
                    if (Modifier.isStatic(mod) || (serializeIgnoreFinalModifier(c) && Modifier.isFinal(mod))) {
                        throw new RuntimeException("Field [" + c.getName() + "." + fn + "] is static/final field.");
                    }
                    // 有 transient 修饰的属性，并且实现 Serializable 接口，则使用 Serialize Builder 对象
                    if (Modifier.isTransient(mod)) {
                        if (iss) {
                            return SerializableBuilder;
                        }
                        // 若没实现 Serializable 接口，抛出 RuntimeException 异常
                        throw new RuntimeException("Field [" + c.getName() + "." + fn + "] is transient field.");
                    }
                    // 添加到结果
                    f.setAccessible(true);
                    fs[i] = f;
                } catch (SecurityException e) {
                    throw new RuntimeException(e.getMessage());
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException("Field [" + c.getName() + "." + fn + "] not found.");
                }
            }
        } else {
            // 反射获得类的所有对象属性
            Class<?> t = c;
            List<Field> fl = new ArrayList<Field>();
            do {
                fs = t.getDeclaredFields();
                for (Field tf : fs) {
                    int mod = tf.getModifiers();
                    // 判断是否符合忽略属性的条件
                    if (Modifier.isStatic(mod) // 忽略静态属性
                            || (serializeIgnoreFinalModifier(c) && Modifier.isFinal(mod))
                            || tf.getName().equals("this$0") // skip static or inner-class's 'this$0' field.
                            || !Modifier.isPublic(tf.getType().getModifiers())) { //skip private inner-class field
                        continue;
                    }
                    // 有 transient 修饰的属性，并且实现 Serializable 接口，则使用 Serialize Builder 对象
                    if (Modifier.isTransient(mod)) {
                        if (iss) {
                            return SerializableBuilder;
                        }
                        continue;
                    }
                    // 添加到结果
                    tf.setAccessible(true); // 设置可访问
                    fl.add(tf);
                }
                // 获得父类
                t = t.getSuperclass();
            } while (t != Object.class);

            // 排序
            fs = fl.toArray(new Field[0]);
            if (fs.length > 1) {
                Arrays.sort(fs, FC);
            }
        }

        // 获得构造方法数组
        // deal with constructors.
        Constructor<?>[] cs = c.getDeclaredConstructors();
        if (cs.length == 0) {
            Class<?> t = c;
            do {
                t = t.getSuperclass();
                if (t == null) { // 若不存在构造方法，抛出 RuntimeException 异常
                    throw new RuntimeException("Can not found Constructor?");
                }
                cs = t.getDeclaredConstructors();
            } while (cs.length == 0);
        }
        // 排序
        if (cs.length > 1) {
            Arrays.sort(cs, CC);
        }

        // `#writeObject(T obj, GenericObjectOutput out)` 抽象方法的实现代码字符串。目前这段是方法头 + 写入对象属性的数量。
        // writeObject code.
        StringBuilder cwf = new StringBuilder("protected void writeObject(Object obj, ").append(GenericObjectOutput.class.getName()).append(" out) throws java.io.IOException{");
        cwf.append(cn).append(" v = (").append(cn).append(")$1; ");
        cwf.append("$2.writeInt(fields.length);");

        // `#readObject(T ret, GenericObjectInput in)` 抽象方法的实现代码字符串。目前这段是方法头 + 读取对象属性的数量。
        // readObject code.
        StringBuilder crf = new StringBuilder("protected void readObject(Object ret, ").append(GenericObjectInput.class.getName()).append(" in) throws java.io.IOException{");
        crf.append("int fc = $2.readInt();");
        crf.append("if( fc != ").append(fs.length).append(" ) throw new IllegalStateException(\"Deserialize Class [").append(cn).append("], field count not matched. Expect ").append(fs.length).append(" but get \" + fc +\".\");");
        crf.append(cn).append(" ret = (").append(cn).append(")$1;");

        // `#newInstance(GenericObjectInput in)` 抽象方法的实现代码字符串
        // newInstance code.
        StringBuilder cni = new StringBuilder("protected Object newInstance(").append(GenericObjectInput.class.getName()).append(" in){ return ");
        Constructor<?> con = cs[0]; // `c` 的第一个构造方法
        int mod = con.getModifiers();
        boolean dn = Modifier.isPublic(mod) || (isp && !Modifier.isPrivate(mod)); // 是否可直接创建
        if (dn) { // 直接创建
            cni.append("new ").append(cn).append("(");
        } else { // 反射创建
            con.setAccessible(true);
            cni.append("constructor.newInstance(new Object[]{");
        }
        // `c` 的构造方法带参数，则拼接相应的参数，并且参数值为默认值。
        Class<?>[] pts = con.getParameterTypes();
        for (int i = 0; i < pts.length; i++) {
            if (i > 0) {
                cni.append(',');
            }
            cni.append(defaultArg(pts[i]));
        }
        // 补充 `#newInstance(GenericObjectInput in)` 的结尾
        if (!dn) {
            cni.append("}"); // close object array.
        }
        cni.append("); }");

        // 获得 PropertyMetadata 集合
        // get bean-style property metadata.
        Map<String, PropertyMetadata> pms = propertyMetadatas(c);
        // 属性对应的 Builder 集合
        List<Builder<?>> builders = new ArrayList<Builder<?>>(fs.length);
        String fn, ftn; // field name, field type name.
        Class<?> ft; // field type.
        boolean da; // direct access.
        PropertyMetadata pm;
        for (int i = 0; i < fs.length; i++) {
            f = fs[i];
            fn = f.getName();
            ft = f.getType();
            ftn = ReflectUtils.getName(ft);
            da = isp && (f.getDeclaringClass() == c) && (!Modifier.isPrivate(f.getModifiers())); // direct access ，可直接访问，无需使用属性的 setting / getting 方法
            if (da) {
                pm = null;
            } else {
                pm = pms.get(fn);
                if (pm != null && (pm.type != ft || pm.setter == null || pm.getter == null)) {
                    pm = null;
                }
            }

            // TODO 【TODO 8035】1、已经限制的大小，这块代码没用了啊？！
            crf.append("if( fc == ").append(i).append(" ) return;");
            // 基本类型，直接 GenericDataOutput 和 GenericDataInput 对应的基础属性的序列化方法。
            if (ft.isPrimitive()) {
                // 添加该属性的在 `#writeObject(T obj, GenericObjectOutput out)` 和 `#readObject(T ret, GenericObjectInput in)` 中的序列化和反序列化的代码
                if (ft == boolean.class) {
                    if (da) { // 直接访问
                        cwf.append("$2.writeBool(v.").append(fn).append(");");
                        crf.append("ret.").append(fn).append(" = $2.readBool();");
                    } else if (pm != null) { // setting/getting 方法访问
                        cwf.append("$2.writeBool(v.").append(pm.getter).append("());");
                        crf.append("ret.").append(pm.setter).append("($2.readBool());");
                    } else { // 反射访问
                        cwf.append("$2.writeBool(((Boolean)fields[").append(i).append("].get($1)).booleanValue());");
                        crf.append("fields[").append(i).append("].set(ret, ($w)$2.readBool());");
                    }
                } else if (ft == byte.class) {
                    if (da) {
                        cwf.append("$2.writeByte(v.").append(fn).append(");");
                        crf.append("ret.").append(fn).append(" = $2.readByte();");
                    } else if (pm != null) {
                        cwf.append("$2.writeByte(v.").append(pm.getter).append("());");
                        crf.append("ret.").append(pm.setter).append("($2.readByte());");
                    } else {
                        cwf.append("$2.writeByte(((Byte)fields[").append(i).append("].get($1)).byteValue());");
                        crf.append("fields[").append(i).append("].set(ret, ($w)$2.readByte());");
                    }
                } else if (ft == char.class) {
                    if (da) {
                        cwf.append("$2.writeShort((short)v.").append(fn).append(");");
                        crf.append("ret.").append(fn).append(" = (char)$2.readShort();");
                    } else if (pm != null) {
                        cwf.append("$2.writeShort((short)v.").append(pm.getter).append("());");
                        crf.append("ret.").append(pm.setter).append("((char)$2.readShort());");
                    } else {
                        cwf.append("$2.writeShort((short)((Character)fields[").append(i).append("].get($1)).charValue());");
                        crf.append("fields[").append(i).append("].set(ret, ($w)((char)$2.readShort()));");
                    }
                } else if (ft == short.class) {
                    if (da) {
                        cwf.append("$2.writeShort(v.").append(fn).append(");");
                        crf.append("ret.").append(fn).append(" = $2.readShort();");
                    } else if (pm != null) {
                        cwf.append("$2.writeShort(v.").append(pm.getter).append("());");
                        crf.append("ret.").append(pm.setter).append("($2.readShort());");
                    } else {
                        cwf.append("$2.writeShort(((Short)fields[").append(i).append("].get($1)).shortValue());");
                        crf.append("fields[").append(i).append("].set(ret, ($w)$2.readShort());");
                    }
                } else if (ft == int.class) {
                    if (da) {
                        cwf.append("$2.writeInt(v.").append(fn).append(");");
                        crf.append("ret.").append(fn).append(" = $2.readInt();");
                    } else if (pm != null) {
                        cwf.append("$2.writeInt(v.").append(pm.getter).append("());");
                        crf.append("ret.").append(pm.setter).append("($2.readInt());");
                    } else {
                        cwf.append("$2.writeInt(((Integer)fields[").append(i).append("].get($1)).intValue());");
                        crf.append("fields[").append(i).append("].set(ret, ($w)$2.readInt());");
                    }
                } else if (ft == long.class) {
                    if (da) {
                        cwf.append("$2.writeLong(v.").append(fn).append(");");
                        crf.append("ret.").append(fn).append(" = $2.readLong();");
                    } else if (pm != null) {
                        cwf.append("$2.writeLong(v.").append(pm.getter).append("());");
                        crf.append("ret.").append(pm.setter).append("($2.readLong());");
                    } else {
                        cwf.append("$2.writeLong(((Long)fields[").append(i).append("].get($1)).longValue());");
                        crf.append("fields[").append(i).append("].set(ret, ($w)$2.readLong());");
                    }
                } else if (ft == float.class) {
                    if (da) {
                        cwf.append("$2.writeFloat(v.").append(fn).append(");");
                        crf.append("ret.").append(fn).append(" = $2.readFloat();");
                    } else if (pm != null) {
                        cwf.append("$2.writeFloat(v.").append(pm.getter).append("());");
                        crf.append("ret.").append(pm.setter).append("($2.readFloat());");
                    } else {
                        cwf.append("$2.writeFloat(((Float)fields[").append(i).append("].get($1)).floatValue());");
                        crf.append("fields[").append(i).append("].set(ret, ($w)$2.readFloat());");
                    }
                } else if (ft == double.class) {
                    if (da) {
                        cwf.append("$2.writeDouble(v.").append(fn).append(");");
                        crf.append("ret.").append(fn).append(" = $2.readDouble();");
                    } else if (pm != null) {
                        cwf.append("$2.writeDouble(v.").append(pm.getter).append("());");
                        crf.append("ret.").append(pm.setter).append("($2.readDouble());");
                    } else {
                        cwf.append("$2.writeDouble(((Double)fields[").append(i).append("].get($1)).doubleValue());");
                        crf.append("fields[").append(i).append("].set(ret, ($w)$2.readDouble());");
                    }
                }
            // 如果属性就是 `c` 类，直接使用 this ，即 `c` 对应的 Builder 对象。
            } else if (ft == c) {
                // 添加该属性的在 `#writeObject(T obj, GenericObjectOutput out)` 和 `#readObject(T ret, GenericObjectInput in)` 中的序列化和反序列化的代码
                if (da) {
                    cwf.append("this.writeTo(v.").append(fn).append(", $2);");
                    crf.append("ret.").append(fn).append(" = (").append(ftn).append(")this.parseFrom($2);");
                } else if (pm != null) {
                    cwf.append("this.writeTo(v.").append(pm.getter).append("(), $2);");
                    crf.append("ret.").append(pm.setter).append("((").append(ftn).append(")this.parseFrom($2));");
                } else {
                    cwf.append("this.writeTo((").append(ftn).append(")fields[").append(i).append("].get($1), $2);");
                    crf.append("fields[").append(i).append("].set(ret, this.parseFrom($2));");
                }
            // 对象，使用对象对应的 Builder 对象
            } else {
                // 获得对象对应的 Builder 对象
                int bc = builders.size();
                builders.add(register(ft)); // 每次注册后，最后一个就是当前属性对应的 Builder 对象
                // 添加该属性的在 `#writeObject(T obj, GenericObjectOutput out)` 和 `#readObject(T ret, GenericObjectInput in)` 中的序列化和反序列化的代码
                if (da) {
                    cwf.append("builders[").append(bc).append("].writeTo(v.").append(fn).append(", $2);");
                    crf.append("ret.").append(fn).append(" = (").append(ftn).append(")builders[").append(bc).append("].parseFrom($2);");
                } else if (pm != null) {
                    cwf.append("builders[").append(bc).append("].writeTo(v.").append(pm.getter).append("(), $2);");
                    crf.append("ret.").append(pm.setter).append("((").append(ftn).append(")builders[").append(bc).append("].parseFrom($2));");
                } else {
                    cwf.append("builders[").append(bc).append("].writeTo((").append(ftn).append(")fields[").append(i).append("].get($1), $2);");
                    crf.append("fields[").append(i).append("].set(ret, builders[").append(bc).append("].parseFrom($2));");
                }
            }
        }

        // TODO 【TODO 8035】1、已经限制的大小，这块代码没用了啊？！
        // skip any fields.
        crf.append("for(int i=").append(fs.length).append(";i<fc;i++) $2.skipAny();");

        // collection or map
        // Collection 子类，添加数组属性的在 `#writeObject(T obj, GenericObjectOutput out)` 和 `#readObject(T ret, GenericObjectInput in)` 中的序列化和反序列化的代码
        if (isc) {
            cwf.append("$2.writeInt(v.size()); for(java.util.Iterator it=v.iterator();it.hasNext();){ $2.writeObject(it.next()); }");
            crf.append("int len = $2.readInt(); for(int i=0;i<len;i++) ret.add($2.readObject());");
        // Map 子类，添加 KV 属性的在 `#writeObject(T obj, GenericObjectOutput out)` 和 `#readObject(T ret, GenericObjectInput in)` 中的序列化和反序列化的代码
        } else if (ism) {
            cwf.append("$2.writeInt(v.size()); for(java.util.Iterator it=v.entrySet().iterator();it.hasNext();){ java.util.Map.Entry entry = (java.util.Map.Entry)it.next(); $2.writeObject(entry.getKey()); $2.writeObject(entry.getValue()); }");
            crf.append("int len = $2.readInt(); for(int i=0;i<len;i++) ret.put($2.readObject(), $2.readObject());");
        }
        cwf.append(" }");
        crf.append(" }");

        // 创建 Builder 代码生成器
        ClassGenerator cg = ClassGenerator.newInstance(cl);
        // 设置类名
        cg.setClassName(bcn);
        // 设置父类为 AbstractObjectBuilder.class
        cg.setSuperClass(AbstractObjectBuilder.class);
        // 添加默认构造方法
        cg.addDefaultConstructor();
        // 添加 `fields` 静态属性
        cg.addField("public static java.lang.reflect.Field[] fields;");
        // 添加 `builders` 静态属性
        cg.addField("public static " + BUILDER_CLASS_NAME + "[] builders;");
        if (!dn) {
            cg.addField("public static java.lang.reflect.Constructor constructor;");
        }
        // 添加 `#getType()` 的实现代码字符串
        cg.addMethod("public Class getType(){ return " + cn + ".class; }");
        // 添加 `#writeObject(T obj, GenericObjectOutput out)` 的实现代码字符串
        cg.addMethod(cwf.toString());
        // 添加 `#readObject(T obj, GenericObjectOutput out)` 的实现代码字符串
        cg.addMethod(crf.toString());
        // 添加 `#newInstance(GenericObjectInput in)` 的实现代码字符串
        cg.addMethod(cni.toString());
        try {
            // 生成类
            Class<?> wc = cg.toClass();
            // 设置 `fields` `builders` 静态属性。
            // set static field
            wc.getField("fields").set(null, fs);
            wc.getField("builders").set(null, builders.toArray(new Builder<?>[0]));
            // 设置构造方法，若需要反射创建对象
            if (!dn) {
                wc.getField("constructor").set(null, con);
            }
            // 创建 Builder 对象
            return (Builder<?>) wc.newInstance();
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            // 释放 ClassGenerator 对象
            cg.release();
        }
    }

    private static Builder<?> newEnumBuilder(Class<?> c) {
        ClassLoader cl = ClassHelper.getCallerClassLoader(Builder.class);
        // 完整类名
        String cn = c.getName();
        // 对应的 Builder 类名
        String bcn = BUILDER_CLASS_NAME + "$bc" + BUILDER_CLASS_COUNTER.getAndIncrement();

        // `#writeObject(T obj, GenericObjectOutput out)` 抽象方法的实现代码字符串。目前这段是方法头 + 写入枚举对应的字符串。
        StringBuilder cwt = new StringBuilder("public void writeTo(Object obj, ").append(GenericObjectOutput.class.getName()).append(" out) throws java.io.IOException{"); // writeTo code.
        cwt.append(cn).append(" v = (").append(cn).append(")$1;");
        cwt.append("if( $1 == null ){ $2.writeUTF(null); }else{ $2.writeUTF(v.name()); } }");

        // `#readObject(T ret, GenericObjectInput in)` 抽象方法的实现代码字符串。目前这段是方法头 + Enum.valueOf(Class, String) 。
        StringBuilder cpf = new StringBuilder("public Object parseFrom(").append(GenericObjectInput.class.getName()).append(" in) throws java.io.IOException{"); // parseFrom code.
        cpf.append("String name = $1.readUTF(); if( name == null ) return null; return (").append(cn).append(")Enum.valueOf(").append(cn).append(".class, name); }");

        // 创建 Builder 代码生成器
        ClassGenerator cg = ClassGenerator.newInstance(cl);
        // 设置类名
        cg.setClassName(bcn);
        // 设置父类为 Builder.class
        cg.setSuperClass(Builder.class);
        // 添加默认构造方法
        cg.addDefaultConstructor();
        cg.addMethod("public Class getType(){ return " + cn + ".class; }");
        cg.addMethod(cwt.toString());
        cg.addMethod(cpf.toString());
        try {
            Class<?> wc = cg.toClass();
            return (Builder<?>) wc.newInstance();
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            cg.release();
        }
    }

    private static Map<String, PropertyMetadata> propertyMetadatas(Class<?> c) {
        // 获得 public 方法集合
        Map<String, Method> mm = new HashMap<String, Method>(); // method map.
        // All public method.
        for (Method m : c.getMethods()) {
            if (m.getDeclaringClass() == Object.class) // Ignore Object's method.
                continue;
            mm.put(ReflectUtils.getDesc(m), m);
        }

        // 获得 PropertyMetadata 集合。KEY 为属性名。
        Map<String, PropertyMetadata> ret = new HashMap<String, PropertyMetadata>(); // property metadata map.
        Matcher matcher;
        for (Map.Entry<String, Method> entry : mm.entrySet()) {
            String desc = entry.getKey();
            Method method = entry.getValue();
            // setting 方法
            if ((matcher = ReflectUtils.GETTER_METHOD_DESC_PATTERN.matcher(desc)).matches() ||
                    (matcher = ReflectUtils.IS_HAS_CAN_METHOD_DESC_PATTERN.matcher(desc)).matches()) {
                String pn = propertyName(matcher.group(1));
                Class<?> pt = method.getReturnType();
                PropertyMetadata pm = ret.get(pn);
                if (pm == null) { // 不存在，则创建 PropertyMetadata 对象
                    pm = new PropertyMetadata();
                    pm.type = pt;
                    ret.put(pn, pm);
                } else {
                    if (pm.type != pt) {
                        continue;
                    }
                }
                pm.getter = method.getName();
            // setting 方法
            } else if ((matcher = ReflectUtils.SETTER_METHOD_DESC_PATTERN.matcher(desc)).matches()) {
                String pn = propertyName(matcher.group(1));
                Class<?> pt = method.getParameterTypes()[0];
                PropertyMetadata pm = ret.get(pn);
                if (pm == null) { // 不存在，则创建 PropertyMetadata 对象
                    pm = new PropertyMetadata();
                    pm.type = pt;
                    ret.put(pn, pm);
                } else {
                    if (pm.type != pt) {
                        continue;
                    }
                }
                pm.setter = method.getName();
            }
        }
        return ret;
    }

    private static String propertyName(String s) {
        return s.length() == 1 || Character.isLowerCase(s.charAt(1)) ? Character.toLowerCase(s.charAt(0)) + s.substring(1) : s;
    }

    private static boolean serializeIgnoreFinalModifier(Class cl) {
//	    if (cl.isAssignableFrom(BigInteger.class)) return false;
//	    for performance
//	    if (cl.getName().startsWith("java")) return true;
//	    if (cl.getName().startsWith("javax")) return true;

        return false;
    }

    @SuppressWarnings("unused")
    private static boolean isPrimitiveOrPrimitiveArray1(Class<?> cl) {
        if (cl.isPrimitive()) {
            return true;
        } else {
            Class clazz = cl.getClass().getComponentType();
            return clazz != null && clazz.isPrimitive();
        }
    }

    private static String defaultArg(Class<?> cl) {
        if (boolean.class == cl) return "false";
        if (int.class == cl) return "0";
        if (long.class == cl) return "0l";
        if (double.class == cl) return "(double)0";
        if (float.class == cl) return "(float)0";
        if (short.class == cl) return "(short)0";
        if (char.class == cl) return "(char)0";
        if (byte.class == cl) return "(byte)0";
        if (byte[].class == cl) return "new byte[]{0}";
        if (!cl.isPrimitive()) return "null";
        throw new UnsupportedOperationException();
    }

    private static int compareFieldName(String n1, String n2) {
        int l = Math.min(n1.length(), n2.length());
        for (int i = 0; i < l; i++) {
            int t = n1.charAt(i) - n2.charAt(i);
            if (t != 0) {
                return t;
            }
        }
        return n1.length() - n2.length();
    }

    /**
     * 添加类描述到集合中
     *
     * @param c 类
     */
    private static void addDesc(Class<?> c) {
        String desc = ReflectUtils.getDesc(c); // 例如，java.lang.Byte 为 Ljava/lang/Byte;
        // 添加到集合中
        int index = mDescList.size();
        mDescList.add(desc);
        mDescMap.put(desc, index);
    }

    // ========== 抽象方法 BEGIN  ==========

    /**
     * @return Builder 对应的类
     */
    abstract public Class<T> getType();

    /**
     * 序列化对象到 GenericObjectOutput 中的输出流。
     *
     * @param obj 对象
     * @param out GenericObjectOutput 对象
     * @throws IOException 当发生 IO 异常时。
     */
    abstract public void writeTo(T obj, GenericObjectOutput out) throws IOException;
    // ↑↑↑ 调用上面方法
    public void writeTo(T obj, OutputStream os) throws IOException {
        // 将 OutputStream 封装成 GenericObjectOutput 对象
        GenericObjectOutput out = new GenericObjectOutput(os);
        // 写入
        writeTo(obj, out);
        // 刷入
        out.flushBuffer();
    }

    /**
     * 反序列化 GenericObjectInput 成对象
     *
     * @param in GenericObjectInput 对象
     * @return 对象
     * @throws IOException 当 IO 发生异常时
     */
    abstract public T parseFrom(GenericObjectInput in) throws IOException;
    // ↑↑↑ 调用上面方法
    public T parseFrom(InputStream is) throws IOException {
        return parseFrom(new GenericObjectInput(is)); // 将 InputStream 封装成 GenericObjectInput 对象
    }
    // ↑↑↑ 调用上面方法
    public T parseFrom(byte[] b) throws IOException {
        return parseFrom(new UnsafeByteArrayInputStream(b)); // 将 byte[] 封装成 InputStream 对象
    }

    // ========== 抽象方法 END  ==========

    /**
     * 属性元数据
     */
    static class PropertyMetadata {

        /**
         * 类型
         */
        Class<?> type;
        /**
         * 设置该属性的方法名
         */
        String setter;
        /**
         * 获得该属性的方法名
         */
        String getter;

    }

    /**
     * Builder 抽象类
     *
     * @param <T> 泛型
     */
    public static abstract class AbstractObjectBuilder<T> extends Builder<T> {

        @Override
        public void writeTo(T obj, GenericObjectOutput out) throws IOException {
            // NULL ，写入 OBJECT_NULL 到 mBuffer 中
            if (obj == null) {
                out.write0(OBJECT_NULL);
            } else {
                // 读取循环引用对象编号
                int ref = out.getRef(obj);
                if (ref < 0) { // 不存在
                    // 添加到循环引用中，从而获得编号。下次在写入相等对象时，可使用循环引用编号的方式。
                    out.addRef(obj);
                    // 写入 OBJECT 到 mBuffer 中
                    out.write0(OBJECT);
                    // 写入 对象 到 mBuffer 中。
                    writeObject(obj, out);
                } else { // 存在
                    // 写入 OBJECT_REF 到 mBuffer 中
                    out.write0(OBJECT_REF);
                    // 写入 循环引用对象编号 到 mBuffer 中
                    out.writeUInt(ref);
                }
            }
        }

        @Override
        public T parseFrom(GenericObjectInput in) throws IOException {
            // 读取首位字节
            byte b = in.read0();
            switch (b) {
                // 对象
                case OBJECT: {
                    // 创建对象
                    T ret = newInstance(in);
                    // 添加到循环引用中，从而获得编号。下次在读取到循环引用对象编号时，可直接获取到该对象。
                    in.addRef(ret);
                    // 反序列化 GenericObjectInput 到对象
                    readObject(ret, in);
                    // 返回
                    return ret;
                }
                // 循环引用对象编号
                case OBJECT_REF:
                    // 读取循环引用对象编号
                    // 获得对应的对象
                    return (T) in.getRef(in.readUInt());
                // NULL ，返回 null
                case OBJECT_NULL:
                    return null;
                default:
                    throw new IOException("Input format error, expect OBJECT|OBJECT_REF|OBJECT_NULL, get " + b);
            }
        }

        /**
         * 创建 Builder 对应类的对象
         *
         * @param in GenericObjectInput 对象
         * @return 对应类的对象
         * @throws IOException 当 IO 发生异常时
         */
        abstract protected T newInstance(GenericObjectInput in) throws IOException;

        /**
         * 序列化对象到 GenericObjectOutput 中的输出流。
         *
         * @param obj 对象
         * @param out GenericObjectOutput 对象
         * @throws IOException 当 IO 发生异常时
         */
        abstract protected void writeObject(T obj, GenericObjectOutput out) throws IOException;

        /**
         * 反序列化 GenericObjectInput 到对象
         *
         * @param ret 对象。
         *            该对象在 {@link #parseFrom(GenericObjectInput)} 中，调用 {@link #newInstance(GenericObjectInput)} 创建
         * @param in GenericObjectInput 对象
         * @throws IOException 当 IO 发生异常时
         */
        abstract protected void readObject(T ret, GenericObjectInput in) throws IOException;

    }

}