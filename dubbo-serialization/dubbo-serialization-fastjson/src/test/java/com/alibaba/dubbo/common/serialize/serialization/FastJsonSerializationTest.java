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
package com.alibaba.dubbo.common.serialize.serialization;

import com.alibaba.dubbo.common.model.media.MediaContent;
import com.alibaba.dubbo.common.serialize.ObjectInput;
import com.alibaba.dubbo.common.serialize.ObjectOutput;
import com.alibaba.dubbo.common.serialize.fastjson.FastJsonSerialization;
import com.alibaba.fastjson.JSONException;

import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayInputStream;

import static org.junit.Assert.fail;

public class FastJsonSerializationTest extends AbstractSerializationPersionOkTest {
    {
        serialization = new FastJsonSerialization();
    }

    @Ignore // FIXME
    @Test
    public void test_BytesRange() throws Exception {
    }

    @Ignore("bool[] type missing to JSONArray")
    @Test
    public void test_boolArray() throws Exception {
    }

    @Ignore("FastJson bug: com.alibaba.fastjson.JSONException: create asm serilizer error, class char")
    @Test
    public void test_charArray() throws Exception {
    }

    @Ignore("FastJson bug: com.alibaba.fastjson.JSONException: create asm serilizer error, class char")
    @Test
    public void test_charArray_withType() throws Exception {
    }

    @Ignore("short[] type missing to JSONArray")
    @Test
    public void test_shortArray() throws Exception {
    }

    @Ignore("int[] type missing to JSONArray")
    @Test
    public void test_intArray() throws Exception {
    }

    @Ignore("long[] type missing to JSONArray")
    @Test
    public void test_longArray() throws Exception {
    }

    @Ignore("float[] type missing to JSONArray")
    @Test
    public void test_floatArray() throws Exception {
    }

    @Ignore("double[] type missing to JSONArray")
    @Test
    public void test_doubleArray() throws Exception {
    }

    @Ignore("String[] type missing to JSONArray")
    @Test
    public void test_StringArray() throws Exception {
    }

    @Ignore("Integer[] type missing to JSONArray")
    @Test
    public void test_IntegerArray() throws Exception {
    }

    @Ignore("Integer[] type missing to JSONArray")
    @Test
    public void test_EnumArray() throws Exception {
    }

    @Ignore("type mising to Long")
    @Test
    public void test_Date() throws Exception {
    }

    @Ignore("type mising to Long")
    @Test
    public void test_Time() throws Exception {
    }

    @Ignore("com.alibaba.fastjson.JSONException: create asm deserializer error, java.sql.Time")
    @Test
    public void test_Time_withType() throws Exception {
    }

    @Ignore("type mising to Integer")
    @Test
    public void test_ByteWrap() throws Exception {
    }

    @Ignore("type mising to Integer")
    @Test
    public void test_LongWrap() throws Exception {
    }

    @Ignore("type mising to Long")
    @Test
    public void test_BigInteger() throws Exception {
    }

    @Ignore("SPerson type missing")
    @Test
    public void test_SPerson() throws Exception {
    }

    @Ignore("BizException type missing to Map")
    @Test
    public void test_BizException() throws Exception {
    }

    @Ignore("BizExceptionNoDefaultConstructor type missing to Map")
    @Test
    public void test_BizExceptionNoDefaultConstructor() throws Exception {
    }

    // FIXME fail when there's no default constructor
    @Ignore("NoDefaultConstructor")
    @Test
    public void test_BizExceptionNoDefaultConstructor_WithType() throws Exception {
    }

    @Ignore("Enum type missing to String")
    @Test
    public void test_enum() throws Exception {
    }

    @Ignore("String set missing to JSONArray")
    @Test
    public void test_StringSet() throws Exception {
    }

    @Ignore("LinkedHashMap type missing to Map")
    @Test
    public void test_LinkedHashMap() throws Exception {
    }


    @Ignore("person type missing")
    @Test
    public void test_SPersonList() throws Exception {
    }

    @Ignore("person type missing")
    @Test
    public void test_SPersonSet() throws Exception {
    }

    @Ignore("FastJson bug: com.alibaba.fastjson.JSONException: illegal identifier : 1")
    @Test
    public void test_IntSPersonMap() throws Exception {
    }

    @Ignore("person type missing")
    @Test
    public void test_StringSPersonMap() throws Exception {
    }

    @Ignore("person type missing")
    @Test
    public void test_StringSPersonListMap() throws Exception {
    }

    @Ignore("person type missing")
    @Test
    public void test_SPersonListList() throws Exception {
    }

    @Ignore("BigPerson type missing")
    @Test
    public void test_BigPerson() throws Exception {
    }

    @Ignore("MediaContent type missing")
    @Test
    public void test_MediaContent() throws Exception {
    }

    @Ignore("type missing")
    @Test
    public void test_MultiObject() throws Exception {
    }

    @Test
    public void test_MediaContent_badStream() throws Exception {
        ObjectOutput objectOutput = serialization.serialize(url, byteArrayOutputStream);
        objectOutput.writeObject(mediaContent);
        objectOutput.flushBuffer();

        byte[] byteArray = byteArrayOutputStream.toByteArray();
        for (int i = 0; i < byteArray.length; i++) {
            if (i % 3 == 0) {
                byteArray[i] = (byte) ~byteArray[i];
            }
        }
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);

        try {
            ObjectInput deserialize = serialization.deserialize(url, byteArrayInputStream);
            @SuppressWarnings("unused") // local variable, convenient for debug
                    Object read = deserialize.readObject();
            fail();
        } catch (JSONException expected) {
            System.out.println(expected);
        }
    }

    @Test
    public void test_MediaContent_WithType_badStream() throws Exception {
        ObjectOutput objectOutput = serialization.serialize(url, byteArrayOutputStream);
        objectOutput.writeObject(mediaContent);
        objectOutput.flushBuffer();

        byte[] byteArray = byteArrayOutputStream.toByteArray();
        for (int i = 0; i < byteArray.length; i++) {
            if (i % 3 == 0) {
                byteArray[i] = (byte) ~byteArray[i];
            }
        }
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);

        try {
            ObjectInput deserialize = serialization.deserialize(url, byteArrayInputStream);
            @SuppressWarnings("unused") // local variable, convenient for debug
                    Object read = deserialize.readObject(MediaContent.class);
            fail();
        } catch (JSONException expected) {
            System.out.println(expected);
        }
    }

    // FIXME DUBBO-63
    @Ignore
    @Test
    public void test_URL_mutable_withType() throws Exception {
    }

    @Ignore
    @Test(timeout = 3000)
    public void test_LoopReference() throws Exception {
    }

    // ========== Person

    @Ignore("person type missing")
    @Test
    public void test_Person() throws Exception {
    }

    @Ignore("person type missing")
    @Test
    public void test_PersonList() throws Exception {
    }

    @Ignore("person type missing")
    @Test
    public void test_PersonSet() throws Exception {
    }

    @Ignore("FastJson bug: com.alibaba.fastjson.JSONException: illegal identifier : 1")
    @Test
    public void test_IntPersonMap() throws Exception {
    }

    @Ignore("person type missing")
    @Test
    public void test_StringPersonMap() throws Exception {
    }

    @Ignore("person type missing")
    @Test
    public void test_StringPersonListMap() throws Exception {
    }

    @Ignore("person type missing")
    @Test
    public void test_PersonListList() throws Exception {
    }

}