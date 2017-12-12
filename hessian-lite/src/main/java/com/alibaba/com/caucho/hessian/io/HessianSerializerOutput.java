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
 * 4. The names "Hessian", "Resin", and "Caucho" must not be used to
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
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Output stream for Hessian requests.
 * <p>
 * <p>HessianOutput is unbuffered, so any client needs to provide
 * its own buffering.
 * <p>
 * <h3>Serialization</h3>
 * <p>
 * <pre>
 * OutputStream os = new FileOutputStream("test.xml");
 * HessianOutput out = new HessianSerializerOutput(os);
 *
 * out.writeObject(obj);
 * os.close();
 * </pre>
 * <p>
 * <h3>Writing an RPC Call</h3>
 * <p>
 * <pre>
 * OutputStream os = ...; // from http connection
 * HessianOutput out = new HessianSerializerOutput(os);
 * String value;
 *
 * out.startCall("hello");  // start hello call
 * out.writeString("arg1"); // write a string argument
 * out.completeCall();      // complete the call
 * </pre>
 */
public class HessianSerializerOutput extends HessianOutput {
    /**
     * Creates a new Hessian output stream, initialized with an
     * underlying output stream.
     *
     * @param os the underlying output stream.
     */
    public HessianSerializerOutput(OutputStream os) {
        super(os);
    }

    /**
     * Creates an uninitialized Hessian output stream.
     */
    public HessianSerializerOutput() {
    }

    /**
     * Applications which override this can do custom serialization.
     *
     * @param object the object to write.
     */
    public void writeObjectImpl(Object obj)
            throws IOException {
        Class cl = obj.getClass();

        try {
            Method method = cl.getMethod("writeReplace", new Class[0]);
            Object repl = method.invoke(obj, new Object[0]);

            writeObject(repl);
            return;
        } catch (Exception e) {
        }

        try {
            writeMapBegin(cl.getName());
            for (; cl != null; cl = cl.getSuperclass()) {
                Field[] fields = cl.getDeclaredFields();
                for (int i = 0; i < fields.length; i++) {
                    Field field = fields[i];

                    if (Modifier.isTransient(field.getModifiers()) ||
                            Modifier.isStatic(field.getModifiers()))
                        continue;

                    // XXX: could parameterize the handler to only deal with public
                    field.setAccessible(true);

                    writeString(field.getName());
                    writeObject(field.get(obj));
                }
            }
            writeMapEnd();
        } catch (IllegalAccessException e) {
            throw new IOExceptionWrapper(e);
        }
    }
}
