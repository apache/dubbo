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
 * @author qian.lei
 */

public class GenericObjectOutput extends GenericDataOutput implements ObjectOutput {
    private final boolean isAllowNonSerializable;
    private ClassDescriptorMapper mMapper;
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void writeObject(Object obj) throws IOException {
        if (obj == null) {
            write0(OBJECT_NULL);
            return;
        }

        Class<?> c = obj.getClass();
        if (c == Object.class) {
            write0(OBJECT_DUMMY);
        } else {
            String desc = ReflectUtils.getDesc(c);
            int index = mMapper.getDescriptorIndex(desc);
            if (index < 0) {
                write0(OBJECT_DESC);
                writeUTF(desc);
            } else {
                write0(OBJECT_DESC_ID);
                writeUInt(index);
            }
            Builder b = Builder.register(c, isAllowNonSerializable);
            b.writeTo(obj, this);
        }
    }

    public void addRef(Object obj) {
        mRefs.put(obj, mRefs.size());
    }

    public int getRef(Object obj) {
        Integer ref = mRefs.get(obj);
        if (ref == null)
            return -1;
        return ref.intValue();
    }
}