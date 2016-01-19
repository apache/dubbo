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
package com.alibaba.dubbo.common.serialize.support.json;

import com.alibaba.dubbo.common.json.Jackson;
import com.alibaba.dubbo.common.serialize.ObjectInput;
import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * JsonObjectInput
 *
 * @author dylan
 */
public class JacksonObjectInput implements ObjectInput {
    private static Logger logger = LoggerFactory.getLogger(JacksonObjectInput.class);

    private final ObjectMapper objectMapper;
    //    private final BufferedReader reader;
    private final Map<String, String> data;
    private static final String KEY_PREFIX = "$";
    private int index = 0;

    public JacksonObjectInput(InputStream inputstream) throws IOException {
//        this.reader = new BufferedReader(new InputStreamReader(inputstream));
//        String line = null;
//        try {
//            while ((line = reader.readLine()) != null) {
//                System.out.println(line);
//            }
//        }catch(Exception e){
//            e.printStackTrace();
//        }
        this.objectMapper = Jackson.getObjectMapper();
        try {
            data = objectMapper.readValue(inputstream, Map.class);
        } catch (IOException e) {
            logger.error("parse inputstream error.", e);
            throw e;
        }
    }

    public boolean readBool() throws IOException {
        try {
            return readObject(Boolean.class);
        } catch (ClassNotFoundException e) {
            throw new IOException(e.getMessage());
        }
    }

    public byte readByte() throws IOException {
        try {
            return readObject(Byte.class);
        } catch (ClassNotFoundException e) {
            throw new IOException(e.getMessage());
        }
    }

    public short readShort() throws IOException {
        try {
            return readObject(Short.class);
        } catch (ClassNotFoundException e) {
            throw new IOException(e.getMessage());
        }
    }

    public int readInt() throws IOException {
        try {
            return readObject(Integer.class);
        } catch (ClassNotFoundException e) {
            throw new IOException(e.getMessage());
        }
    }

    public long readLong() throws IOException {
        try {
            return readObject(Long.class);
        } catch (ClassNotFoundException e) {
            throw new IOException(e.getMessage());
        }
    }

    public float readFloat() throws IOException {
        try {
            return readObject(Float.class);
        } catch (ClassNotFoundException e) {
            throw new IOException(e.getMessage());
        }
    }

    public double readDouble() throws IOException {
        try {
            return readObject(Double.class);
        } catch (ClassNotFoundException e) {
            throw new IOException(e.getMessage());
        }
    }

    public String readUTF() throws IOException {
        try {
            return readObject(String.class);
        } catch (ClassNotFoundException e) {
            throw new IOException(e.getMessage());
        }
    }

    public byte[] readBytes() throws IOException {
        return readUTF().getBytes();
    }

    public Object readObject() throws IOException, ClassNotFoundException {
//        try {
//            String json = readLine();
//            if (json.startsWith("{")) {
//                return JSON.parse(json, Map.class);
//            } else {
//                json = "{\"value\":" + json + "}";
//
//                @SuppressWarnings("unchecked")
//                Map<String, Object> map = objectMapper.readValue(json, Map.class);
//                return map.get("value");
//            }
//        } catch (ParseException e) {
//            throw new IOException(e.getMessage());
//        }
        try {
            return readObject(Object.class);
        } catch (ClassNotFoundException e) {
            throw new IOException(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T readObject(Class<T> cls) throws IOException, ClassNotFoundException {
//        Object value = readObject();
        //read data value
        String json = this.data.get(KEY_PREFIX + (++index));
        //read data type
        String dataType = this.data.get(KEY_PREFIX + index + "t");
        if (dataType != null) {
            Class clazz = ReflectUtils.desc2class(dataType);
            if (cls.isAssignableFrom(clazz)) {
                cls = clazz;
            } else {
                throw new IllegalArgumentException("Class \"" + clazz + "\" is not inherited from \"" + cls + "\"");
            }
        }
        logger.debug("index:{}, value:{}", index, json);
        return objectMapper.readValue(json, cls);
    }

    @SuppressWarnings("unchecked")
    public <T> T readObject(Class<T> cls, Type type) throws IOException, ClassNotFoundException {
//        Object value = readObject();
        return readObject(cls);
    }

}