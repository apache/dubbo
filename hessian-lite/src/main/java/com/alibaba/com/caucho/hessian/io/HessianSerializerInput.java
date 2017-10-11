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
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Input stream for Hessian requests, deserializing objects using the
 * java.io.Serialization protocol.
 * <p>
 * <p>HessianSerializerInput is unbuffered, so any client needs to provide
 * its own buffering.
 * <p>
 * <h3>Serialization</h3>
 * <p>
 * <pre>
 * InputStream is = new FileInputStream("test.xml");
 * HessianOutput in = new HessianSerializerOutput(is);
 *
 * Object obj = in.readObject();
 * is.close();
 * </pre>
 * <p>
 * <h3>Parsing a Hessian reply</h3>
 * <p>
 * <pre>
 * InputStream is = ...; // from http connection
 * HessianInput in = new HessianSerializerInput(is);
 * String value;
 *
 * in.startReply();         // read reply header
 * value = in.readString(); // read string value
 * in.completeReply();      // read reply footer
 * </pre>
 */
public class HessianSerializerInput extends HessianInput {
    /**
     * Creates a new Hessian input stream, initialized with an
     * underlying input stream.
     *
     * @param is the underlying input stream.
     */
    public HessianSerializerInput(InputStream is) {
        super(is);
    }

    /**
     * Creates an uninitialized Hessian input stream.
     */
    public HessianSerializerInput() {
    }

    /**
     * Reads an object from the input stream.  cl is known not to be
     * a Map.
     */
    protected Object readObjectImpl(Class cl)
            throws IOException {
        try {
            Object obj = cl.newInstance();

            if (_refs == null)
                _refs = new ArrayList();
            _refs.add(obj);

            HashMap fieldMap = getFieldMap(cl);

            int code = read();
            for (; code >= 0 && code != 'z'; code = read()) {
                _peek = code;

                Object key = readObject();

                Field field = (Field) fieldMap.get(key);

                if (field != null) {
                    Object value = readObject(field.getType());
                    field.set(obj, value);
                } else {
                    Object value = readObject();
                }
            }

            if (code != 'z')
                throw expect("map", code);

            // if there's a readResolve method, call it
            try {
                Method method = cl.getMethod("readResolve", new Class[0]);
                return method.invoke(obj, new Object[0]);
            } catch (Exception e) {
            }

            return obj;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOExceptionWrapper(e);
        }
    }

    /**
     * Creates a map of the classes fields.
     */
    protected HashMap getFieldMap(Class cl) {
        HashMap fieldMap = new HashMap();

        for (; cl != null; cl = cl.getSuperclass()) {
            Field[] fields = cl.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                Field field = fields[i];

                if (Modifier.isTransient(field.getModifiers()) ||
                        Modifier.isStatic(field.getModifiers()))
                    continue;

                // XXX: could parameterize the handler to only deal with public
                field.setAccessible(true);

                fieldMap.put(field.getName(), field);
            }
        }

        return fieldMap;
    }
}
