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
package com.alibaba.dubbo.rpc.cluster.merger;

import com.alibaba.dubbo.rpc.cluster.Merger;

import java.lang.reflect.Array;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class ArrayMerger implements Merger<Object> {

    public static final String      NAME     = "array";

    public static final ArrayMerger INSTANCE = new ArrayMerger();

    public Object merge(Object... others) {
        if (others.length == 0) {
            return null;
        }
        int totalLen = 0;
        for(int i = 0; i < others.length; i++) {
            Object item = others[i];
            if (item != null && item.getClass().isArray()) {
                totalLen += Array.getLength(item);
            } else {
                throw new IllegalArgumentException(
                        new StringBuilder(32).append(i + 1)
                                .append("th argument is not an array").toString());
            }
        }

        if (totalLen == 0) { return null;}

        Class<?> type = others[0].getClass().getComponentType();

        Object result = Array.newInstance(type, totalLen);
        int index = 0;
        if (boolean.class == type) {
            mergeArray(result, BOOLEAN_ARRAY_FILLER, others);
        } else if (char.class == type) {
            mergeArray(result, CHAR_ARRAY_FILLER, others);
        } else if (byte.class == type) {
            mergeArray(result, BYTE_ARRAY_FILLER, others);
        } else if (short.class == type) {
            mergeArray(result, SHORT_ARRAY_FILLER, others);
        } else if (int.class == type) {
            mergeArray(result, INT_ARRAY_FILLER, others);
        } else if (long.class == type) {
            mergeArray(result, LONG_ARRAY_FILLER, others);
        } else if (float.class == type) {
            mergeArray(result, FLOAT_ARRAY_FILLER, others);
        } else if (double.class == type) {
            mergeArray(result, DOUBLE_ARRAY_FILLER, others);
        } else {
            mergeArray(result, OBJECT_ARRAY_FILLER, others);
        }
        return result;
    }
    
    static interface ArrayFiller {
        void fill(Object in, int index, Object out);
    }

    private static final ArrayFiller BOOLEAN_ARRAY_FILLER = new ArrayFiller() {

        public void fill(Object in, int index, Object out) {
            int len = Array.getLength( out );
            for( int i = 0; i < len; i++) {
                Array.setBoolean(in, index++, Array.getBoolean(out, i));
            }
        }
    };

    private static final ArrayFiller CHAR_ARRAY_FILLER = new ArrayFiller() {

        public void fill(Object in, int index, Object out) {
            int len = Array.getLength(out);
            for (int i = 0; i < len; i++) {
                Array.setChar(in, index++, Array.getChar(out, i));
            }
        }
    };

    private static final ArrayFiller BYTE_ARRAY_FILLER = new ArrayFiller() {

        public void fill(Object in, int index, Object out) {
            int len = Array.getLength(out);
            for (int i = 0; i < len; i++) {
                Array.setByte(in, index++, Array.getByte(out, i));
            }
        }
    };

    private static final ArrayFiller SHORT_ARRAY_FILLER = new ArrayFiller() {

        public void fill(Object in, int index, Object out) {
            int len = Array.getLength(out);
            for (int i = 0; i < len; i++) {
                Array.setShort(in, index++, Array.getShort(out, i));
            }
        }
    };

    private static final ArrayFiller INT_ARRAY_FILLER = new ArrayFiller() {

        public void fill(Object in, int index, Object out) {
            int len = Array.getLength(out);
            for (int i = 0; i < len; i++) {
                Array.setInt(in, index++, Array.getInt(out, i));
            }
        }
    };

    private static final ArrayFiller LONG_ARRAY_FILLER = new ArrayFiller() {

        public void fill(Object in, int index, Object out) {
            int len = Array.getLength(out);
            for (int i = 0; i < len; i++) {
                Array.setLong(in, index++, Array.getLong(out, i));
            }
        }
    };

    private static final ArrayFiller FLOAT_ARRAY_FILLER = new ArrayFiller() {

        public void fill(Object in, int index, Object out) {
            int len = Array.getLength(out);
            for (int i = 0; i < len; i++) {
                Array.setFloat(in, index++, Array.getFloat(out, i));
            }
        }
    };

    private static final ArrayFiller DOUBLE_ARRAY_FILLER = new ArrayFiller() {

        public void fill(Object in, int index, Object out) {
            int len = Array.getLength(out);
            for (int i = 0; i < len; i++) {
                Array.setDouble(in, index++, Array.getDouble(out, i));
            }
        }
    };

    private static final ArrayFiller OBJECT_ARRAY_FILLER = new ArrayFiller() {

        public void fill(Object in, int index, Object out) {
            int len = Array.getLength(out);
            for (int i = 0; i < len; i++) {
                Array.set(in, index++, Array.get(out, i));
            }
        }
    };
    
    static void mergeArray(Object result, ArrayFiller filler, Object ... others) {
        int index = 0;
        for(int i = 0; i < others.length; i++) {
            Object item = others[i];
            if (item != null) {
                filler.fill(result, index, item);
                index += Array.getLength(item);
            }
        }
    }

}
