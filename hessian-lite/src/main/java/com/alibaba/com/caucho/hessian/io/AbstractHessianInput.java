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
import java.io.Reader;

/**
 * Abstract base class for Hessian requests.  Hessian users should only
 * need to use the methods in this class.
 * <p>
 * <pre>
 * AbstractHessianInput in = ...; // get input
 * String value;
 *
 * in.startReply();         // read reply header
 * value = in.readString(); // read string value
 * in.completeReply();      // read reply footer
 * </pre>
 */
abstract public class AbstractHessianInput {
    private HessianRemoteResolver resolver;

    /**
     * Initialize the Hessian stream with the underlying input stream.
     */
    public void init(InputStream is) {
    }

    /**
     * Returns the call's method
     */
    abstract public String getMethod();

    /**
     * Sets the resolver used to lookup remote objects.
     */
    public HessianRemoteResolver getRemoteResolver() {
        return resolver;
    }

    /**
     * Sets the resolver used to lookup remote objects.
     */
    public void setRemoteResolver(HessianRemoteResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * Sets the serializer factory.
     */
    public void setSerializerFactory(SerializerFactory ser) {
    }

    /**
     * Reads the call
     * <p>
     * <pre>
     * c major minor
     * </pre>
     */
    abstract public int readCall()
            throws IOException;

    /**
     * For backward compatibility with HessianSkeleton
     */
    public void skipOptionalCall()
            throws IOException {
    }

    /**
     * Reads a header, returning null if there are no headers.
     * <p>
     * <pre>
     * H b16 b8 value
     * </pre>
     */
    abstract public String readHeader()
            throws IOException;

    /**
     * Starts reading the call
     * <p>
     * <p>A successful completion will have a single value:
     * <p>
     * <pre>
     * m b16 b8 method
     * </pre>
     */
    abstract public String readMethod()
            throws IOException;

    /**
     * Reads the number of method arguments
     *
     * @return -1 for a variable length (hessian 1.0)
     */
    public int readMethodArgLength()
            throws IOException {
        return -1;
    }

    /**
     * Starts reading the call, including the headers.
     * <p>
     * <p>The call expects the following protocol data
     * <p>
     * <pre>
     * c major minor
     * m b16 b8 method
     * </pre>
     */
    abstract public void startCall()
            throws IOException;

    /**
     * Completes reading the call
     * <p>
     * <p>The call expects the following protocol data
     * <p>
     * <pre>
     * Z
     * </pre>
     */
    abstract public void completeCall()
            throws IOException;

    /**
     * Reads a reply as an object.
     * If the reply has a fault, throws the exception.
     */
    abstract public Object readReply(Class expectedClass)
            throws Throwable;

    /**
     * Starts reading the reply
     * <p>
     * <p>A successful completion will have a single value:
     * <p>
     * <pre>
     * r
     * v
     * </pre>
     */
    abstract public void startReply()
            throws Throwable;

    /**
     * Completes reading the call
     * <p>
     * <p>A successful completion will have a single value:
     * <p>
     * <pre>
     * z
     * </pre>
     */
    abstract public void completeReply()
            throws IOException;

    /**
     * Reads a boolean
     * <p>
     * <pre>
     * T
     * F
     * </pre>
     */
    abstract public boolean readBoolean()
            throws IOException;

    /**
     * Reads a null
     * <p>
     * <pre>
     * N
     * </pre>
     */
    abstract public void readNull()
            throws IOException;

    /**
     * Reads an integer
     * <p>
     * <pre>
     * I b32 b24 b16 b8
     * </pre>
     */
    abstract public int readInt()
            throws IOException;

    /**
     * Reads a long
     * <p>
     * <pre>
     * L b64 b56 b48 b40 b32 b24 b16 b8
     * </pre>
     */
    abstract public long readLong()
            throws IOException;

    /**
     * Reads a double.
     * <p>
     * <pre>
     * D b64 b56 b48 b40 b32 b24 b16 b8
     * </pre>
     */
    abstract public double readDouble()
            throws IOException;

    /**
     * Reads a date.
     * <p>
     * <pre>
     * T b64 b56 b48 b40 b32 b24 b16 b8
     * </pre>
     */
    abstract public long readUTCDate()
            throws IOException;

    /**
     * Reads a string encoded in UTF-8
     * <p>
     * <pre>
     * s b16 b8 non-final string chunk
     * S b16 b8 final string chunk
     * </pre>
     */
    abstract public String readString()
            throws IOException;

    /**
     * Reads an XML node encoded in UTF-8
     * <p>
     * <pre>
     * x b16 b8 non-final xml chunk
     * X b16 b8 final xml chunk
     * </pre>
     */
    public org.w3c.dom.Node readNode()
            throws IOException {
        throw new UnsupportedOperationException(getClass().getSimpleName());
    }

    /**
     * Starts reading a string.  All the characters must be read before
     * calling the next method.  The actual characters will be read with
     * the reader's read() or read(char [], int, int).
     * <p>
     * <pre>
     * s b16 b8 non-final string chunk
     * S b16 b8 final string chunk
     * </pre>
     */
    abstract public Reader getReader()
            throws IOException;

    /**
     * Starts reading a byte array using an input stream.  All the bytes
     * must be read before calling the following method.
     * <p>
     * <pre>
     * b b16 b8 non-final binary chunk
     * B b16 b8 final binary chunk
     * </pre>
     */
    abstract public InputStream readInputStream()
            throws IOException;

    /**
     * Reads a byte array.
     * <p>
     * <pre>
     * b b16 b8 non-final binary chunk
     * B b16 b8 final binary chunk
     * </pre>
     */
    abstract public byte[] readBytes()
            throws IOException;

    /**
     * Reads an arbitrary object from the input stream.
     *
     * @param expectedClass the expected class if the protocol doesn't supply it.
     */
    abstract public Object readObject(Class expectedClass)
            throws IOException;

    /**
     * Reads an arbitrary object from the input stream.
     */
    abstract public Object readObject()
            throws IOException;

    /**
     * Reads a remote object reference to the stream.  The type is the
     * type of the remote interface.
     * <p>
     * <code><pre>
     * 'r' 't' b16 b8 type url
     * </pre></code>
     */
    abstract public Object readRemote()
            throws IOException;

    /**
     * Reads a reference
     * <p>
     * <pre>
     * R b32 b24 b16 b8
     * </pre>
     */
    abstract public Object readRef()
            throws IOException;

    /**
     * Adds an object reference.
     */
    abstract public int addRef(Object obj)
            throws IOException;

    /**
     * Sets an object reference.
     */
    abstract public void setRef(int i, Object obj)
            throws IOException;

    /**
     * Resets the references for streaming.
     */
    public void resetReferences() {
    }

    /**
     * Reads the start of a list
     */
    abstract public int readListStart()
            throws IOException;

    /**
     * Reads the length of a list.
     */
    abstract public int readLength()
            throws IOException;

    /**
     * Reads the start of a map
     */
    abstract public int readMapStart()
            throws IOException;

    /**
     * Reads an object type.
     */
    abstract public String readType()
            throws IOException;

    /**
     * Returns true if the data has ended.
     */
    abstract public boolean isEnd()
            throws IOException;

    /**
     * Read the end byte
     */
    abstract public void readEnd()
            throws IOException;

    /**
     * Read the end byte
     */
    abstract public void readMapEnd()
            throws IOException;

    /**
     * Read the end byte
     */
    abstract public void readListEnd()
            throws IOException;

    public void close()
            throws IOException {
    }
}
