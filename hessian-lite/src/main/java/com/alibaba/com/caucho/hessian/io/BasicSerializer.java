/*
 * Copyright (c) 2001-2004 Caucho Technology, Inc.  All rights reserved.
 *
 * The Apache Software License, Version 1.1
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Caucho Technology (http://www.caucho.com/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Burlap", "Resin", and "Caucho" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    info@caucho.com.
 *
 * 5. Products derived from this software may not be called "Resin"
 *    nor may "Resin" appear in their names without prior written
 *    permission of Caucho Technology.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL CAUCHO TECHNOLOGY OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Scott Ferguson
 */

package com.alibaba.com.caucho.hessian.io;

import java.io.IOException;
import java.util.Date;

/**
 * Serializing an object for known object types.
 */
public class BasicSerializer extends AbstractSerializer {
    public static final int NULL = 0;
    public static final int BOOLEAN = NULL + 1;
    public static final int BYTE = BOOLEAN + 1;
    public static final int SHORT = BYTE + 1;
    public static final int INTEGER = SHORT + 1;
    public static final int LONG = INTEGER + 1;
    public static final int FLOAT = LONG + 1;
    public static final int DOUBLE = FLOAT + 1;
    public static final int CHARACTER = DOUBLE + 1;
    public static final int CHARACTER_OBJECT = CHARACTER + 1;
    public static final int STRING = CHARACTER_OBJECT + 1;
    public static final int DATE = STRING + 1;
    public static final int NUMBER = DATE + 1;
    public static final int OBJECT = NUMBER + 1;

    public static final int BOOLEAN_ARRAY = OBJECT + 1;
    public static final int BYTE_ARRAY = BOOLEAN_ARRAY + 1;
    public static final int SHORT_ARRAY = BYTE_ARRAY + 1;
    public static final int INTEGER_ARRAY = SHORT_ARRAY + 1;
    public static final int LONG_ARRAY = INTEGER_ARRAY + 1;
    public static final int FLOAT_ARRAY = LONG_ARRAY + 1;
    public static final int DOUBLE_ARRAY = FLOAT_ARRAY + 1;
    public static final int CHARACTER_ARRAY = DOUBLE_ARRAY + 1;
    public static final int STRING_ARRAY = CHARACTER_ARRAY + 1;
    public static final int OBJECT_ARRAY = STRING_ARRAY + 1;

    private int code;

    public BasicSerializer(int code) {
        this.code = code;
    }

    public void writeObject(Object obj, AbstractHessianOutput out)
            throws IOException {
        switch (code) {
            case BOOLEAN:
                out.writeBoolean(((Boolean) obj).booleanValue());
                break;

            case BYTE:
            case SHORT:
            case INTEGER:
                out.writeInt(((Number) obj).intValue());
                break;

            case LONG:
                out.writeLong(((Number) obj).longValue());
                break;

            case FLOAT:
            case DOUBLE:
                out.writeDouble(((Number) obj).doubleValue());
                break;

            case CHARACTER:
            case CHARACTER_OBJECT:
                out.writeString(String.valueOf(obj));
                break;

            case STRING:
                out.writeString((String) obj);
                break;

            case DATE:
                out.writeUTCDate(((Date) obj).getTime());
                break;

            case BOOLEAN_ARRAY: {
                if (out.addRef(obj))
                    return;

                boolean[] data = (boolean[]) obj;
                boolean hasEnd = out.writeListBegin(data.length, "[boolean");
                for (int i = 0; i < data.length; i++)
                    out.writeBoolean(data[i]);

                if (hasEnd)
                    out.writeListEnd();

                break;
            }

            case BYTE_ARRAY: {
                byte[] data = (byte[]) obj;
                out.writeBytes(data, 0, data.length);
                break;
            }

            case SHORT_ARRAY: {
                if (out.addRef(obj))
                    return;

                short[] data = (short[]) obj;
                boolean hasEnd = out.writeListBegin(data.length, "[short");

                for (int i = 0; i < data.length; i++)
                    out.writeInt(data[i]);

                if (hasEnd)
                    out.writeListEnd();
                break;
            }

            case INTEGER_ARRAY: {
                if (out.addRef(obj))
                    return;

                int[] data = (int[]) obj;

                boolean hasEnd = out.writeListBegin(data.length, "[int");

                for (int i = 0; i < data.length; i++)
                    out.writeInt(data[i]);

                if (hasEnd)
                    out.writeListEnd();

                break;
            }

            case LONG_ARRAY: {
                if (out.addRef(obj))
                    return;

                long[] data = (long[]) obj;

                boolean hasEnd = out.writeListBegin(data.length, "[long");

                for (int i = 0; i < data.length; i++)
                    out.writeLong(data[i]);

                if (hasEnd)
                    out.writeListEnd();
                break;
            }

            case FLOAT_ARRAY: {
                if (out.addRef(obj))
                    return;

                float[] data = (float[]) obj;

                boolean hasEnd = out.writeListBegin(data.length, "[float");

                for (int i = 0; i < data.length; i++)
                    out.writeDouble(data[i]);

                if (hasEnd)
                    out.writeListEnd();
                break;
            }

            case DOUBLE_ARRAY: {
                if (out.addRef(obj))
                    return;

                double[] data = (double[]) obj;
                boolean hasEnd = out.writeListBegin(data.length, "[double");

                for (int i = 0; i < data.length; i++)
                    out.writeDouble(data[i]);

                if (hasEnd)
                    out.writeListEnd();
                break;
            }

            case STRING_ARRAY: {
                if (out.addRef(obj))
                    return;

                String[] data = (String[]) obj;

                boolean hasEnd = out.writeListBegin(data.length, "[string");

                for (int i = 0; i < data.length; i++) {
                    out.writeString(data[i]);
                }

                if (hasEnd)
                    out.writeListEnd();
                break;
            }

            case CHARACTER_ARRAY: {
                char[] data = (char[]) obj;
                out.writeString(data, 0, data.length);
                break;
            }

            case OBJECT_ARRAY: {
                if (out.addRef(obj))
                    return;

                Object[] data = (Object[]) obj;

                boolean hasEnd = out.writeListBegin(data.length, "[object");

                for (int i = 0; i < data.length; i++) {
                    out.writeObject(data[i]);
                }

                if (hasEnd)
                    out.writeListEnd();
                break;
            }

            case NULL:
                out.writeNull();
                break;

            default:
                throw new RuntimeException(code + " " + String.valueOf(obj.getClass()));
        }
    }
}
