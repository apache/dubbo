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

import com.alibaba.dubbo.common.serialize.ObjectOutput;
import com.alibaba.dubbo.common.utils.ReflectUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic Object Output.
 *
 * Dubbo 对象输出实现类
 */
public class GenericObjectOutput extends GenericDataOutput implements ObjectOutput {

    /**
     * 对象是否允许不实现 {@link java.io.Serializable} 接口
     */
    private final boolean isAllowNonSerializable;
    /**
     * 类描述匹配器
     */
    private ClassDescriptorMapper mMapper;
    /**
     * 循环引用集合
     *
     * KEY ：对象
     * VALUE ：引用编号
     */
    private Map<Object, Integer> mRefs = new ConcurrentHashMap<Object, Integer>();

    public GenericObjectOutput(OutputStream out) {
        this(out, Builder.DEFAULT_CLASS_DESCRIPTOR_MAPPER);
    }

    public GenericObjectOutput(OutputStream out, ClassDescriptorMapper mapper) {
        super(out);
        mMapper = mapper;
        isAllowNonSerializable = false;
    }

    public GenericObjectOutput(OutputStream out, int buffSize) {
        this(out, buffSize, Builder.DEFAULT_CLASS_DESCRIPTOR_MAPPER, false);
    }

    public GenericObjectOutput(OutputStream out, int buffSize, ClassDescriptorMapper mapper) {
        this(out, buffSize, mapper, false);
    }

    public GenericObjectOutput(OutputStream out, int buffSize, ClassDescriptorMapper mapper, boolean isAllowNonSerializable) {
        super(out, buffSize);
        mMapper = mapper;
        this.isAllowNonSerializable = isAllowNonSerializable;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void writeObject(Object obj) throws IOException {
        // NULL ，使用 OBJECT_NULL 写入 mBuffer
        if (obj == null) {
            write0(OBJECT_NULL);
            return;
        }
        // 空对象，使用 OBJECT_DUMMY 写入 mBuffer
        Class<?> c = obj.getClass();
        if (c == Object.class) {
            write0(OBJECT_DUMMY);
        } else {
            // 获得类描述
            String desc = ReflectUtils.getDesc(c);
            // 查询类描述编号
            int index = mMapper.getDescriptorIndex(desc);
            // 不存在，使用 OBJECT_DESC + 类描述 写入 mBuffer
            if (index < 0) {
                write0(OBJECT_DESC);
                writeUTF(desc);
            // 存在，使用 OBJECT_DESC_ID + 类描述编号 写入 mBuffer
            } else {
                write0(OBJECT_DESC_ID);
                writeUInt(index);
            }
            // 获得类对应的序列化 Builder
            Builder b = Builder.register(c, isAllowNonSerializable);
            // 序列化到 mBuffer 中
            b.writeTo(obj, this);
        }
    }

    /**
     * 添加循环引用
     *
     * @param obj 对象
     */
    public void addRef(Object obj) {
        mRefs.put(obj, mRefs.size() /** 引用编号 **/ );
    }

    /**
     * 获得循环引用编号
     *
     * @param obj 对象
     * @return 引用编号
     */
    public int getRef(Object obj) {
        Integer ref = mRefs.get(obj);
        if (ref == null) {
            return -1;
        }
        return ref;
    }

}