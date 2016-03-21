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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

	private static final int TYPE_DFT = -1;
	private static final int TYPE_MAP = 0;
	private static final int TYPE_LIST = 1;
	private static final int TYPE_SET = 2;

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

	public static int bytesToInt(byte[] src, int offset) {
		int value;
		value = (int) ((src[offset] & 0xFF) | ((src[offset + 1] & 0xFF) << 8) | ((src[offset + 2] & 0xFF) << 16));
		return value;
	}

	public Object readObject() throws IOException, ClassNotFoundException {
		if (logger.isDebugEnabled()) {
			logger.debug("iam now in readObject without args");
		}
		// returntype == null 或者抛异常的时候返回。已经被我修改源码，理论上不会走到这。
		throw new UnsupportedOperationException();

	}

	public <T> T readObject(Class<T> cls) throws IOException,
			ClassNotFoundException {
		if (logger.isDebugEnabled()) {
			logger.debug("now in read Object Class method..." + cls.getName());
		}
		Schema schema = null;
		Object content = null;
		byte[] length = new byte[3];
		inputStream.read(length);

		byte[] bodys = new byte[bytesToInt(length, 0)];

		int type = -1;
		if (Map.class.isAssignableFrom(cls)) {
			schema = RuntimeSchema.getSchema(MapEntity.class);
			content = new MapEntity();
			type = TYPE_MAP;
		} else if (Collection.class.isAssignableFrom(cls)) {
			if (cls.isAssignableFrom(List.class)) {
				schema = RuntimeSchema.getSchema(ListEntity.class);
				content = new ListEntity();
				type = TYPE_LIST;
			} else if (Set.class.isAssignableFrom(cls)) {
				schema = RuntimeSchema.getSchema(SetEntity.class);
				content = new SetEntity();
				type = TYPE_SET;
			}
		} else if (Integer.class.isAssignableFrom(cls)
				|| Integer.TYPE.isAssignableFrom(cls)) {
			schema = RuntimeSchema.getSchema(Integer.class);
			content = new Integer(0);
		} else if (Boolean.class.isAssignableFrom(cls)
				|| Boolean.TYPE.isAssignableFrom(cls)) {
			schema = RuntimeSchema.getSchema(Boolean.class);
			content = new Boolean(false);
		} else if (Short.class.isAssignableFrom(cls)
				|| Short.TYPE.isAssignableFrom(cls)) {
			schema = RuntimeSchema.getSchema(Short.class);
			content = new Short("0");
		} else if (Long.class.isAssignableFrom(cls)
				|| Long.TYPE.isAssignableFrom(cls)) {
			schema = RuntimeSchema.getSchema(Long.class);
			content = new Long(0L);
		} else if (Double.class.isAssignableFrom(cls)
				|| Double.TYPE.isAssignableFrom(cls)) {
			schema = RuntimeSchema.getSchema(Double.class);
			content = new Double(0);
		} else if (Float.class.isAssignableFrom(cls)
				|| Float.TYPE.isAssignableFrom(cls)) {
			schema = RuntimeSchema.getSchema(Float.class);
			content = new Float(0);
		} else if (Character.class.isAssignableFrom(cls)
				|| Character.TYPE.isAssignableFrom(cls)) {
			schema = RuntimeSchema.getSchema(Character.class);
			content = new Character('0');
		} else {
			schema = RuntimeSchema.getSchema(cls);
			try {
				content = (T) cls.newInstance();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		inputStream.read(bodys);
		ProtostuffUtils.mergeFrom(bodys, content, schema);

		switch (type) {
		case TYPE_DFT:
			return (T) content;
		case TYPE_MAP:
			return (T) ((MapEntity) content).getData();
		case TYPE_LIST:
			return (T) ((ListEntity) content).getData();
		case TYPE_SET:
			return (T) ((SetEntity) content).getData();
		default:
			throw new RuntimeException();
		}

	}

	public <T> T readObject(Class<T> cls, Type type) throws IOException,
			ClassNotFoundException {
		if (logger.isDebugEnabled()) {
			logger.debug("now in read Object Class And Type method...");
		}
		return (T) readObject(cls);
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