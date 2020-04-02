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
package org.apache.dubbo.common.json;

import org.apache.dubbo.common.bytecode.Wrapper;
import org.apache.dubbo.common.io.Bytes;

import java.io.IOException;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Deprecated
public class GenericJSONConverter implements JSONConverter {
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final Map<Class<?>, Encoder> GLOBAL_ENCODER_MAP = new HashMap<Class<?>, Encoder>();
    private static final Map<Class<?>, Decoder> GLOBAL_DECODER_MAP = new HashMap<Class<?>, Decoder>();

    static {
        // init encoder map.
        Encoder e = new Encoder() {
            @Override
            public void encode(Object obj, JSONWriter jb) throws IOException {
                jb.valueBoolean((Boolean) obj);
            }
        };
        GLOBAL_ENCODER_MAP.put(boolean.class, e);
        GLOBAL_ENCODER_MAP.put(Boolean.class, e);

        e = new Encoder() {
            @Override
            public void encode(Object obj, JSONWriter jb) throws IOException {
                jb.valueInt(((Number) obj).intValue());
            }
        };
        GLOBAL_ENCODER_MAP.put(int.class, e);
        GLOBAL_ENCODER_MAP.put(Integer.class, e);
        GLOBAL_ENCODER_MAP.put(short.class, e);
        GLOBAL_ENCODER_MAP.put(Short.class, e);
        GLOBAL_ENCODER_MAP.put(byte.class, e);
        GLOBAL_ENCODER_MAP.put(Byte.class, e);
        GLOBAL_ENCODER_MAP.put(AtomicInteger.class, e);

        e = new Encoder() {
            @Override
            public void encode(Object obj, JSONWriter jb) throws IOException {
                jb.valueString(Character.toString((Character) obj));
            }
        };
        GLOBAL_ENCODER_MAP.put(char.class, e);
        GLOBAL_ENCODER_MAP.put(Character.class, e);

        e = new Encoder() {
            @Override
            public void encode(Object obj, JSONWriter jb) throws IOException {
                jb.valueLong(((Number) obj).longValue());
            }
        };
        GLOBAL_ENCODER_MAP.put(long.class, e);
        GLOBAL_ENCODER_MAP.put(Long.class, e);
        GLOBAL_ENCODER_MAP.put(AtomicLong.class, e);
        GLOBAL_ENCODER_MAP.put(BigInteger.class, e);

        e = new Encoder() {
            @Override
            public void encode(Object obj, JSONWriter jb) throws IOException {
                jb.valueFloat(((Number) obj).floatValue());
            }
        };
        GLOBAL_ENCODER_MAP.put(float.class, e);
        GLOBAL_ENCODER_MAP.put(Float.class, e);

        e = new Encoder() {
            @Override
            public void encode(Object obj, JSONWriter jb) throws IOException {
                jb.valueDouble(((Number) obj).doubleValue());
            }
        };
        GLOBAL_ENCODER_MAP.put(double.class, e);
        GLOBAL_ENCODER_MAP.put(Double.class, e);
        GLOBAL_ENCODER_MAP.put(BigDecimal.class, e);

        e = new Encoder() {
            @Override
            public void encode(Object obj, JSONWriter jb) throws IOException {
                jb.valueString(obj.toString());
            }
        };
        GLOBAL_ENCODER_MAP.put(String.class, e);
        GLOBAL_ENCODER_MAP.put(StringBuilder.class, e);
        GLOBAL_ENCODER_MAP.put(StringBuffer.class, e);

        e = new Encoder() {
            @Override
            public void encode(Object obj, JSONWriter jb) throws IOException {
                jb.valueString(Bytes.bytes2base64((byte[]) obj));
            }
        };
        GLOBAL_ENCODER_MAP.put(byte[].class, e);

        e = new Encoder() {
            @Override
            public void encode(Object obj, JSONWriter jb) throws IOException {
                jb.valueString(new SimpleDateFormat(DATE_FORMAT).format((Date) obj));
            }
        };
        GLOBAL_ENCODER_MAP.put(Date.class, e);

        // init decoder map.
        Decoder d = Object::toString;
        GLOBAL_DECODER_MAP.put(String.class, d);

        d = new Decoder() {
            @Override
            public Object decode(Object jv) {
                if (jv instanceof Boolean) {
                    return ((Boolean) jv).booleanValue();
                }
                return false;
            }
        };
        GLOBAL_DECODER_MAP.put(boolean.class, d);

        d = new Decoder() {
            @Override
            public Object decode(Object jv) {
                if (jv instanceof Boolean) {
                    return (Boolean) jv;
                }
                return (Boolean) null;
            }
        };
        GLOBAL_DECODER_MAP.put(Boolean.class, d);

        d = new Decoder() {
            @Override
            public Object decode(Object jv) {
                if (jv instanceof String && ((String) jv).length() > 0) {
                    return ((String) jv).charAt(0);
                }
                return (char) 0;
            }
        };
        GLOBAL_DECODER_MAP.put(char.class, d);

        d = new Decoder() {
            @Override
            public Object decode(Object jv) {
                if (jv instanceof String && ((String) jv).length() > 0) {
                    return ((String) jv).charAt(0);
                }
                return (Character) null;
            }
        };
        GLOBAL_DECODER_MAP.put(Character.class, d);

        d = new Decoder() {
            @Override
            public Object decode(Object jv) {
                if (jv instanceof Number) {
                    return ((Number) jv).intValue();
                }
                return 0;
            }
        };
        GLOBAL_DECODER_MAP.put(int.class, d);

        d = new Decoder() {
            @Override
            public Object decode(Object jv) {
                if (jv instanceof Number) {
                    return Integer.valueOf(((Number) jv).intValue());
                }
                return (Integer) null;
            }
        };
        GLOBAL_DECODER_MAP.put(Integer.class, d);

        d = new Decoder() {
            @Override
            public Object decode(Object jv) {
                if (jv instanceof Number) {
                    return ((Number) jv).shortValue();
                }
                return (short) 0;
            }
        };
        GLOBAL_DECODER_MAP.put(short.class, d);

        d = new Decoder() {
            @Override
            public Object decode(Object jv) {
                if (jv instanceof Number) {
                    return Short.valueOf(((Number) jv).shortValue());
                }
                return (Short) null;
            }
        };
        GLOBAL_DECODER_MAP.put(Short.class, d);

        d = new Decoder() {
            @Override
            public Object decode(Object jv) {
                if (jv instanceof Number) {
                    return ((Number) jv).longValue();
                }
                return (long) 0;
            }
        };
        GLOBAL_DECODER_MAP.put(long.class, d);

        d = new Decoder() {
            @Override
            public Object decode(Object jv) {
                if (jv instanceof Number) {
                    return Long.valueOf(((Number) jv).longValue());
                }
                return (Long) null;
            }
        };
        GLOBAL_DECODER_MAP.put(Long.class, d);

        d = new Decoder() {
            @Override
            public Object decode(Object jv) {
                if (jv instanceof Number) {
                    return ((Number) jv).floatValue();
                }
                return (float) 0;
            }
        };
        GLOBAL_DECODER_MAP.put(float.class, d);

        d = new Decoder() {
            @Override
            public Object decode(Object jv) {
                if (jv instanceof Number) {
                    return new Float(((Number) jv).floatValue());
                }
                return (Float) null;
            }
        };
        GLOBAL_DECODER_MAP.put(Float.class, d);

        d = new Decoder() {
            @Override
            public Object decode(Object jv) {
                if (jv instanceof Number) {
                    return ((Number) jv).doubleValue();
                }
                return (double) 0;
            }
        };
        GLOBAL_DECODER_MAP.put(double.class, d);

        d = new Decoder() {
            @Override
            public Object decode(Object jv) {
                if (jv instanceof Number) {
                    return new Double(((Number) jv).doubleValue());
                }
                return (Double) null;
            }
        };
        GLOBAL_DECODER_MAP.put(Double.class, d);

        d = new Decoder() {
            @Override
            public Object decode(Object jv) {
                if (jv instanceof Number) {
                    return ((Number) jv).byteValue();
                }
                return (byte) 0;
            }
        };
        GLOBAL_DECODER_MAP.put(byte.class, d);

        d = new Decoder() {
            @Override
            public Object decode(Object jv) {
                if (jv instanceof Number) {
                    return Byte.valueOf(((Number) jv).byteValue());
                }
                return (Byte) null;
            }
        };
        GLOBAL_DECODER_MAP.put(Byte.class, d);

        d = new Decoder() {
            @Override
            public Object decode(Object jv) throws IOException {
                if (jv instanceof String) {
                    return Bytes.base642bytes((String) jv);
                }
                return (byte[]) null;
            }
        };
        GLOBAL_DECODER_MAP.put(byte[].class, d);

        d = new Decoder() {
            @Override
            public Object decode(Object jv) throws IOException {
                return new StringBuilder(jv.toString());
            }
        };
        GLOBAL_DECODER_MAP.put(StringBuilder.class, d);

        d = new Decoder() {
            @Override
            public Object decode(Object jv) throws IOException {
                return new StringBuffer(jv.toString());
            }
        };
        GLOBAL_DECODER_MAP.put(StringBuffer.class, d);

        d = new Decoder() {
            @Override
            public Object decode(Object jv) throws IOException {
                if (jv instanceof Number) {
                    return BigInteger.valueOf(((Number) jv).longValue());
                }
                return (BigInteger) null;
            }
        };
        GLOBAL_DECODER_MAP.put(BigInteger.class, d);

        d = new Decoder() {
            @Override
            public Object decode(Object jv) throws IOException {
                if (jv instanceof Number) {
                    return BigDecimal.valueOf(((Number) jv).doubleValue());
                }
                return (BigDecimal) null;
            }
        };
        GLOBAL_DECODER_MAP.put(BigDecimal.class, d);

        d = new Decoder() {
            @Override
            public Object decode(Object jv) throws IOException {
                if (jv instanceof Number) {
                    return new AtomicInteger(((Number) jv).intValue());
                }
                return (AtomicInteger) null;
            }
        };
        GLOBAL_DECODER_MAP.put(AtomicInteger.class, d);

        d = new Decoder() {
            @Override
            public Object decode(Object jv) throws IOException {
                if (jv instanceof Number) {
                    return new AtomicLong(((Number) jv).longValue());
                }
                return (AtomicLong) null;
            }
        };
        GLOBAL_DECODER_MAP.put(AtomicLong.class, d);

        d = new Decoder() {
            @Override
            public Object decode(Object jv) throws IOException {
                if (jv instanceof String) {
                    try {
                        return new SimpleDateFormat(DATE_FORMAT).parse((String) jv);
                    } catch (ParseException e) {
                        throw new IllegalArgumentException(e.getMessage(), e);
                    }
                }
                if (jv instanceof Number) {
                    return new Date(((Number) jv).longValue());
                }
                return (Date) null;
            }
        };
        GLOBAL_DECODER_MAP.put(Date.class, d);

        d = new Decoder() {
            @Override
            public Object decode(Object jv) throws IOException {
                if (jv instanceof String) {
                    String[] items = ((String)jv).split("_");
                    if(items.length == 1){
                        return new Locale(items[0]);
                    }
                    if(items.length == 2){
                        return new Locale(items[0], items[1]);
                    }
                    return new Locale(items[0], items[1], items[2]);
                }
                return (Locale)null;
            }
        };
        GLOBAL_DECODER_MAP.put(Locale.class, d);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void writeValue(Object obj, JSONWriter jb, boolean writeClass) throws IOException {
        if (obj == null) {
            jb.valueNull();
            return;
        }
        Class<?> c = obj.getClass();
        Encoder encoder = GLOBAL_ENCODER_MAP.get(c);

        if (encoder != null) {
            encoder.encode(obj, jb);
        } else if (obj instanceof JSONNode) {
            ((JSONNode) obj).writeJSON(this, jb, writeClass);
        } else if (c.isEnum()) {
            jb.valueString(((Enum<?>) obj).name());
        } else if (c.isArray()) {
            int len = Array.getLength(obj);
            jb.arrayBegin();
            for (int i = 0; i < len; i++) {
                writeValue(Array.get(obj, i), jb, writeClass);
            }
            jb.arrayEnd();
        } else if (Map.class.isAssignableFrom(c)) {
            Object key, value;
            jb.objectBegin();
            for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) obj).entrySet()) {
                key = entry.getKey();
                if (key == null) {
                    continue;
                }
                jb.objectItem(key.toString());

                value = entry.getValue();
                if (value == null) {
                    jb.valueNull();
                } else {
                    writeValue(value, jb, writeClass);
                }
            }
            jb.objectEnd();
        } else if (Collection.class.isAssignableFrom(c)) {
            jb.arrayBegin();
            for (Object item : (Collection<Object>) obj) {
                if (item == null) {
                    jb.valueNull();
                } else {
                    writeValue(item, jb, writeClass);
                }
            }
            jb.arrayEnd();
        } else if(obj instanceof Locale) {
            jb.valueString(obj.toString());
        } else {
            jb.objectBegin();

            Wrapper w = Wrapper.getWrapper(c);
            String[] pns = w.getPropertyNames();

            for (String pn : pns) {
                if ((obj instanceof Throwable) && (
                        "localizedMessage".equals(pn)
                                || "cause".equals(pn)
                                || "suppressed".equals(pn)
                                || "stackTrace".equals(pn))) {
                    continue;
                }

                jb.objectItem(pn);

                Object value = w.getPropertyValue(obj, pn);
                if (value == null || value == obj) {
                    jb.valueNull();
                } else {
                    writeValue(value, jb, writeClass);
                }
            }
            if (writeClass) {
                jb.objectItem(JSONVisitor.CLASS_PROPERTY);
                writeValue(obj.getClass().getName(), jb, writeClass);
            }
            jb.objectEnd();
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object readValue(Class<?> c, Object jv) throws IOException {
        if (jv == null) {
            return null;
        }
        Decoder decoder = GLOBAL_DECODER_MAP.get(c);
        if (decoder != null) {
            return decoder.decode(jv);
        }
        if (c.isEnum()) {
            return Enum.valueOf((Class<Enum>) c, String.valueOf(jv));
        }
        return jv;
    }

    protected interface Encoder {
        void encode(Object obj, JSONWriter jb) throws IOException;
    }

    protected interface Decoder {
        Object decode(Object jv) throws IOException;
    }
}
