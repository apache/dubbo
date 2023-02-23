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
package org.apache.dubbo.rpc.protocol.rest.util;

import org.apache.dubbo.common.utils.JsonUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class DataParseUtils {

    public static Object stringTypeConvert(Class targetType, String value) {


        if (targetType == Boolean.class) {
            return Boolean.valueOf(value);
        }

        if (targetType == String.class) {
            return value;
        }

        if (Number.class.isAssignableFrom(targetType)) {
            return NumberUtils.parseNumber(value, targetType);
        }

        return value;

    }


    /**
     * content-type text
     *
     * @param object
     * @param outputStream
     * @throws IOException
     */
    public static void writeTextContent(Object object, OutputStream outputStream) throws IOException {
        outputStream.write(objectTextConvertToByteArray(object));
    }

    /**
     * content-type json
     *
     * @param object
     * @param outputStream
     * @throws Exception
     */
    public static void writeJsonContent(Object object, OutputStream outputStream) throws Exception {
        outputStream.write(JsonUtils.getJson().toJson(object).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * content-type form
     *
     * @param formData
     * @param outputStream
     * @throws Exception
     */
    public static void writeFormContent(Map formData, OutputStream outputStream) throws Exception {
        outputStream.write(serializeForm(formData, Charset.defaultCharset()).getBytes());
    }

    // TODO file multipart

    public static String serializeForm(Map formData, Charset charset) {
        StringBuilder builder = new StringBuilder();
        formData.forEach((name, values) -> {
            if (name == null) {

                return;
            }
            ((List) values).forEach(value -> {
                try {
                    if (builder.length() != 0) {
                        builder.append('&');
                    }
                    builder.append(URLEncoder.encode((String) name, charset.name()));
                    if (value != null) {
                        builder.append('=');
                        builder.append(URLEncoder.encode(String.valueOf(value), charset.name()));
                    }
                } catch (UnsupportedEncodingException ex) {
                    throw new IllegalStateException(ex);
                }
            });
        });

        return builder.toString();
    }

    public static byte[] objectTextConvertToByteArray(Object object) {
        Class<?> objectClass = object.getClass();

        if (objectClass == Boolean.class) {
            return object.toString().getBytes();
        }

        if (objectClass == String.class) {
            return ((String) object).getBytes();
        }

        if (objectClass.isAssignableFrom(Number.class)) {
            return (byte[]) NumberUtils.numberToBytes((Number) object);
        }

        return object.toString().getBytes();

    }

    public static byte[] objectJsonConvertToByteArray(Object object) {
        Class<?> objectClass = object.getClass();

        if (objectClass == Boolean.class) {
            return object.toString().getBytes();
        }

        if (objectClass == String.class) {
            return ((String) object).getBytes();
        }

        if (objectClass.isAssignableFrom(Number.class)) {
            return (byte[]) NumberUtils.numberToBytes((Number) object);
        }

        return object.toString().getBytes();

    }

    public static Object jsonConvert(Class targetType, byte[] body) throws Exception {
        return JsonUtils.getJson().toJavaObject(new String(body, StandardCharsets.UTF_8), targetType);
    }


    public static Object multipartFormConvert(byte[] body, Charset charset) throws Exception {
        String[] pairs = tokenizeToStringArray(new String(body, StandardCharsets.UTF_8), "&");
        Object result = MultiValueCreator.createMultiValueMap();
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx == -1) {
                MultiValueCreator.add(result, URLDecoder.decode(pair, charset.name()), null);
            } else {
                String name = URLDecoder.decode(pair.substring(0, idx), charset.name());
                String value = URLDecoder.decode(pair.substring(idx + 1), charset.name());
                MultiValueCreator.add(result, name, value);
            }
        }

        return result;
    }

    public static Object multipartFormConvert(byte[] body) throws Exception {
        return multipartFormConvert(body, Charset.defaultCharset());
    }


    public static String[] tokenizeToStringArray(String str, String delimiters) {
        return tokenizeToStringArray(str, delimiters, true, true);
    }

    public static String[] tokenizeToStringArray(String str, String delimiters, boolean trimTokens,
                                                 boolean ignoreEmptyTokens) {
        if (str == null) {
            return null;
        } else {
            StringTokenizer st = new StringTokenizer(str, delimiters);
            ArrayList tokens = new ArrayList();

            while (true) {
                String token;
                do {
                    if (!st.hasMoreTokens()) {
                        return toStringArray(tokens);
                    }

                    token = st.nextToken();
                    if (trimTokens) {
                        token = token.trim();
                    }
                } while (ignoreEmptyTokens && token.length() <= 0);

                tokens.add(token);
            }
        }
    }

    public static String[] toStringArray(Collection<String> collection) {
        return collection == null ? null : collection.toArray(new String[collection.size()]);
    }
}
