/*
 * Copyright 2011 Alibaba.com All right reserved. This software is the
 * confidential and proprietary information of Alibaba.com ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with Alibaba.com.
 */
package com.dubbo.serialize.benchmark;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.alibaba.dubbo.common.io.UnsafeByteArrayInputStream;
import com.alibaba.dubbo.common.io.UnsafeByteArrayOutputStream;
import com.alibaba.dubbo.common.serialize.support.dubbo.GenericObjectInput;
import com.alibaba.dubbo.common.serialize.support.dubbo.GenericObjectOutput;
import com.caucho.hessian.io.Hessian2StreamingInput;
import com.caucho.hessian.io.Hessian2StreamingOutput;

import data.media.MediaContent;

/**
 * 类Dubbo.java的实现描述：Dubbo Seriazition Benchmark
 * 
 * @author tony.chenl 2011-9-30 上午10:17:21
 */
public class Dubbo {

    public static void register(TestGroups groups) {
        groups.media.add(JavaBuiltIn.MediaTransformer, Dubbo.<MediaContent>GenericSerializer());
    }
    
    public static <T> Serializer<T> GenericSerializer()
    {
        @SuppressWarnings("unchecked")
        Serializer<T> s = (Serializer<T>) GenericSerializer;
        return s;
    }

    // ------------------------------------------------------------
    // Serializer (just one)

    public static Serializer<Object> GenericSerializer = new Serializer<Object>()
    {
        public Object deserialize(byte[] array) throws Exception
        {
            GenericObjectInput objectInput = new GenericObjectInput(new ByteArrayInputStream(array));
            return objectInput.readObject();
        }

        public byte[] serialize(Object data) throws java.io.IOException
        {
            UnsafeByteArrayOutputStream os = new UnsafeByteArrayOutputStream(10240);
            GenericObjectOutput objectOutput = new GenericObjectOutput(os);
            objectOutput.writeObject(data);
            objectOutput.flushBuffer();
            return os.toByteArray();
        }

        public String getName()
        {
            return "dubbo";
        }
    };
}
