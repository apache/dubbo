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
import java.io.InputStream;
import java.lang.reflect.Type;

import org.apache.log4j.Logger;

import com.alibaba.dubbo.common.serialize.Cleanable;
import com.alibaba.dubbo.common.serialize.ObjectInput;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;

/**
 * 
 * @author surlymo
 *
 */
public class ProtoObjectInput implements ObjectInput, Cleanable {

	private InputStream inputStream;

	private static final Logger logger = Logger
			.getLogger(ProtoObjectOutput.class);
	
	public ProtoObjectInput(InputStream inputStream) {
		this.inputStream = inputStream;
	}

	public byte readByte() throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("now in readByte method...");
		}
		byte[] bytes = new byte[1];
		inputStream.read(bytes, 0, 1);
		return bytes[0];
	}

	public String readUTF() throws IOException {

		byte[] bytes = new byte[1];
		inputStream.read(bytes, 0, 1);
		int size = (int) bytes[0];

		byte[] bodyBytes = new byte[size];
		inputStream.read(bodyBytes, 0, size);

		if (logger.isDebugEnabled()) {
			logger.debug("now in readUTF method...");
			StringBuilder builder = new StringBuilder();
			builder.append("the bytes is: ");
			for (byte b : bodyBytes) {
				builder.append(b + " ");
			}
			logger.debug(builder.toString());
			logger.debug("the value is: " + new String(bodyBytes));
		}

		return new String(bodyBytes);
	}

	public Object readObject() throws IOException, ClassNotFoundException {
		byte[] cntByte = new byte[1];
		inputStream.read(cntByte);
		int globalSize = cntByte[0];
		if(globalSize < 0){
			globalSize &= 0x00ff;
		}
		byte[] bs = new byte[globalSize];
		int len = inputStream.read(bs);

		// 获取class名 & class对象
		int classSize = bs[0];
		byte[] className = new byte[classSize];
		for (int i = 1; i < classSize + 1; i++) {
			className[i - 1] = bs[i];
		}
		String classRealName = new String(className);
		Class realClass = Class.forName(classRealName);

		// 获取body字节流
		int bodySize = bs[classSize + 1];
		if(bodySize < 0){
			bodySize &= 0x00ff;
		}
		byte[] bodys = new byte[bodySize];
		for (int j = 0, i = 2 + classSize; j < bodySize; i++, j++) {
			bodys[j] = bs[i];
		}

		Schema schema = RuntimeSchema.getSchema(realClass);
		Object content = null;
		try {
			content = realClass.newInstance();
		} catch (Exception e) {
			logger.error(e);
		}

		if (logger.isDebugEnabled()) {
			logger.debug("============read begin===========");
			logger.debug("total length is: " + len);
			logger.debug("className is: " + classRealName);
			logger.debug("classSize: " + classSize);
			logger.debug("body length is: " + bodySize);
			StringBuilder builder = new StringBuilder();
			builder.append("body bytes array: ");
			for (byte b : bodys) {
				builder.append(b + " ");
			}
			logger.debug(builder.toString());
			logger.debug("Instance: " + content.toString());
			logger.debug("the object after decode is: " + content);
			logger.debug("============read end===========");
		}

		ProtostuffUtils.mergeFrom(bodys, content, schema);
		return content;

	}

	public <T> T readObject(Class<T> cls) throws IOException,
			ClassNotFoundException {
		if (logger.isDebugEnabled()) {
			logger.debug("now in read Object Class method..." + cls.getName());
		}
		return (T) readObject();
	}

	public <T> T readObject(Class<T> cls, Type type) throws IOException,
			ClassNotFoundException {
		if (logger.isDebugEnabled()) {
			logger.debug("now in read Object Class And Type method...");
		}
		return (T) readObject();
	}

	@Deprecated
	public short readShort() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Deprecated
	public int readInt() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Deprecated
	public long readLong() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Deprecated
	public float readFloat() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Deprecated
	public double readDouble() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Deprecated
	public boolean readBool() throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Deprecated
	public byte[] readBytes() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Deprecated
	public void cleanup() {
		// TODO Auto-generated method stub

	}

}