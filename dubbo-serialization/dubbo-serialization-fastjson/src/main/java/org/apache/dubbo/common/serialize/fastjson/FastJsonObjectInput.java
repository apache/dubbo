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
package org.apache.dubbo.common.serialize.fastjson;

import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.ParserConfig;
import org.apache.dubbo.common.serialize.ObjectInput;

import com.alibaba.fastjson.JSON;
import org.apache.dubbo.common.utils.PojoUtils;
import org.apache.dubbo.common.utils.ReflectUtils;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

/**
 * FastJson object input implementation
 */
public class FastJsonObjectInput implements ObjectInput {

    private final BufferedReader reader;

    public FastJsonObjectInput(InputStream in) {
        this(new InputStreamReader(in));
    }

    public FastJsonObjectInput(Reader reader) {
        this.reader = new BufferedReader(reader);
    }

    @Override
    public boolean readBool() throws IOException {
        return read(boolean.class);
    }

    @Override
    public byte readByte() throws IOException {
        return read(byte.class);
    }

    @Override
    public short readShort() throws IOException {
        return read(short.class);
    }

    @Override
    public int readInt() throws IOException {
        return read(int.class);
    }

    @Override
    public long readLong() throws IOException {
        return read(long.class);
    }

    @Override
    public float readFloat() throws IOException {
        return read(float.class);
    }

    @Override
    public double readDouble() throws IOException {
        return read(double.class);
    }

    @Override
    public String readUTF() throws IOException {
        return read(String.class);
    }

    @Override
    public byte[] readBytes() throws IOException {
        return readLine().getBytes();
    }

    @Override
    public Object readObject() throws IOException, ClassNotFoundException {
        String json = readLine();
        if (json == null) {
            return null;
        }
        DefaultJSONParser parser = new DefaultJSONParser(json, getDubboFastjsonConfigParseConfig(), JSON.DEFAULT_PARSER_FEATURE);
        Object value = parser.parse();
        parser.handleResovleTask(value);
        parser.close();

        return value;
    }

    @Override
    public <T> T readObject(Class<T> cls) throws IOException, ClassNotFoundException {
        return read(cls);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T readObject(Class<T> cls, Type type) throws IOException, ClassNotFoundException {
        Object value = null;
        if (isConvertToJsonByType(cls, type)) {
            String json = readLine();
            value = JSON.parseObject(json, type);
        } else {
            value = readObject(cls, type);
            return (T) value;
        }
        return (T) PojoUtils.realize(value, cls, type);
    }

    private static ParserConfig dubboFastjsonConfig =null;

    private static ParserConfig getDubboFastjsonConfigParseConfig(){
        if (dubboFastjsonConfig==null){
            ParserConfig newConfig =new ParserConfig();
            ParserConfig globalInstance = ParserConfig.getGlobalInstance();
            Field[] declaredFields = ParserConfig.class.getDeclaredFields();
            for (Field field : declaredFields) {
                if (!field.isAccessible()){
                    field.setAccessible(true);
                }
                try {
                    Object globalValue = field.get(globalInstance);
                    field.set(newConfig, globalValue);
                } catch (IllegalArgumentException e) {
                    //skip
                } catch (IllegalAccessException e) {
                    //skip
                }
            }
            //必须是AutoTypeSupport=true
            newConfig.setAutoTypeSupport(true);
            dubboFastjsonConfig =newConfig;
        }
        return dubboFastjsonConfig;
    }

    private String readLine() throws IOException, EOFException {
        String line = reader.readLine();
        if (line == null || line.trim().length() == 0) {
            throw new EOFException();
        }
        return line;
    }

    private <T> T read(Class<T> cls) throws IOException {
        String json = readLine();
        return JSON.parseObject(json, cls);
    }


    protected boolean isConvertToJsonByType(Class<?> clz, Type type) {
        if (clz == null) {
            return false;
        }

        if (type == null) {
            return false;
        }

        if (clz.isEnum()) {
            return false;
        }

        if (ReflectUtils.isPrimitives(clz)) {
            return false;
        }

        if (clz.isArray()) {
            return false;
        }
        if (Collection.class.isAssignableFrom(clz)) {
            return false;
        }

        if (Map.class.isAssignableFrom(clz)) {
            return false;
        }
        return true;
    }

}
