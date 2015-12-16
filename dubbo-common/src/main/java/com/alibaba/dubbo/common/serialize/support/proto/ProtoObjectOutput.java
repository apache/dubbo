/**
 * Copyright 1999-2014 dangdang.com.
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
package com.alibaba.dubbo.common.serialize.support.proto;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.log4j.Logger;

import com.alibaba.dubbo.common.serialize.Cleanable;
import com.alibaba.dubbo.common.serialize.ObjectOutput;
import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;

/**
 * TODO  
 * 1. 写入的class可以和dubbo默认写入的class整合下
 * 2. 将现在写入的三个记录长度的数据改为两个。一共需要1+3=4个字节。
 * 3. 进行小于0的判断。byte在传递时会变成-128-127。
 * @author lishen
 */
public class ProtoObjectOutput implements ObjectOutput, Cleanable {

    public final OutputStream stream;
    
    private static final Logger logger = Logger.getLogger(ProtoObjectOutput.class);

    private ThreadLocal<LinkedBuffer> linkedBuffer = new ThreadLocal<LinkedBuffer>() {
        @Override
        protected LinkedBuffer initialValue() {
            return LinkedBuffer.allocate(500);
        }
    };

    public ProtoObjectOutput(OutputStream stream) {
        this.stream = stream;
    }

    public void writeObject(Object obj) throws IOException {
        try {
        	
        	// 获取class字节数组
            byte[] classBytes = obj.getClass().getName().getBytes();
            
            // 获取body字节数组
//            System.out.println("begin proto core.." + System.currentTimeMillis());
            Schema schema = RuntimeSchema.getSchema(obj.getClass());
            byte[] bodyBytes = ProtostuffUtils.toByteArray(obj, schema, linkedBuffer.get());
//            System.out.println("finish proto core.." + System.currentTimeMillis());
            
            // 获取class字节长度
            int classSize = classBytes.length;
            
            // 获取body字节长度
            int bodySize = bodyBytes.length;

            // 总长度。包含1个总长度、1个class长度、1个body长度、以及class全限定名和body内容字节数组
            int totalLength = 1 + 2 + classSize + bodySize;

            byte[] result = new byte[totalLength];
            result[0] = (byte) (totalLength - 1);
            result[1] = (byte)classSize;
            for (int i = 2, j = 0; i <= classSize + 1; i++,j++) {
                result[i] = classBytes[j];
            }
            result[2 + classSize] = (byte)bodySize;
            for (int i = 3 + classSize, j = 0; i < result.length - 1; i++,j++) {
                result[i] = bodyBytes[j];
            }

            if(logger.isDebugEnabled()){
            	logger.debug("===========write begin===================");
        		logger.debug("obj class is: " + obj.getClass().getName());
        		
        		logger.debug("size of class: " + classSize);
            	logger.debug("real class name: " + new String(classBytes));
            	logger.debug("size of body: " + bodyBytes.length);
        		
            	logger.debug("size of result: " + result.length);
            	StringBuilder builder = new StringBuilder();
            	builder.append("result bytes array: ");
            	for (byte b : result) {
            		builder.append(b + " ");
                 }
            	logger.debug(builder.toString());
            	logger.debug("===========write end===================");
            }
            stream.write(result);

        } finally {
            linkedBuffer.get().clear();
        }
    }

    public void writeByte(byte v) throws IOException {
    	if(logger.isDebugEnabled()){
    		logger.debug("now in writeBytes method... ");
    	}
        stream.write(v);
    }

    public void writeUTF(String v) throws IOException {
    	if(logger.isDebugEnabled()){
    		logger.debug("now in writeUTF method... ");
    	}
        // 按照先存大小，后存内容的方式存储。
        stream.write(v.length());
        stream.write(v.getBytes());
    }

    public void flushBuffer() throws IOException {
        stream.flush();
    }

    @Deprecated
    public void writeBool(boolean v) throws IOException {
        writeObject(v);

    }

    @Deprecated
    public void writeShort(short v) throws IOException {
        writeObject(v);
    }

    @Deprecated
    public void writeInt(int v) throws IOException {
        writeObject(v);
    }

    @Deprecated
    public void writeLong(long v) throws IOException {
        writeObject(v);
    }

    @Deprecated
    public void writeFloat(float v) throws IOException {
        writeObject(v);
    }

    @Deprecated
    public void writeDouble(double v) throws IOException {
        writeObject(v);

    }

    @Deprecated
    public void writeBytes(byte[] v) throws IOException {
        // TODO Auto-generated method stub

    }

    @Deprecated
    public void writeBytes(byte[] v, int off, int len) throws IOException {
        // TODO Auto-generated method stub

    }

    @Deprecated
    public void cleanup() {
        // TODO Auto-generated method stub

    }

}