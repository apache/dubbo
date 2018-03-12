/*
 * Copyright (c) 2001-2008 Caucho Technology, Inc.  All rights reserved.
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
import java.io.OutputStream;
import java.util.IdentityHashMap;

/**
 * Output stream for Hessian requests, compatible with microedition
 * Java.  It only uses classes and types available in JDK.
 * <p>
 * <p>Since HessianOutput does not depend on any classes other than
 * in the JDK, it can be extracted independently into a smaller package.
 * <p>
 * <p>HessianOutput is unbuffered, so any client needs to provide
 * its own buffering.
 * <p>
 * <pre>
 * OutputStream os = ...; // from http connection
 * HessianOutput out = new HessianOutput(os);
 * String value;
 *
 * out.startCall("hello");  // start hello call
 * out.writeString("arg1"); // write a string argument
 * out.completeCall();      // complete the call
 * </pre>
 */
public class HessianOutput extends AbstractHessianOutput {
    // the output stream/
    protected OutputStream os;
    // map of references
    private IdentityHashMap _refs;
    private int _version = 1;

    /**
     * Creates a new Hessian output stream, initialized with an
     * underlying output stream.
     *
     * @param os the underlying output stream.
     */
    public HessianOutput(OutputStream os) {
        init(os);
    }

    /**
     * Creates an uninitialized Hessian output stream.
     */
    public HessianOutput() {
    }

    /**
     * Initializes the output
     */
    public void init(OutputStream os) {
        this.os = os;

        _refs = null;

        if (_serializerFactory == null)
            _serializerFactory = new SerializerFactory();
    }

    /**
     * Sets the client's version.
     */
    public void setVersion(int version) {
        _version = version;
    }

    /**
     * Writes a complete method call.
     */
    public void call(String method, Object[] args)
            throws IOException {
        int length = args != null ? args.length : 0;

        startCall(method, length);

        for (int i = 0; i < length; i++)
            writeObject(args[i]);

        completeCall();
    }

    /**
     * Starts the method call.  Clients would use <code>startCall</code>
     * instead of <code>call</code> if they wanted finer control over
     * writing the arguments, or needed to write headers.
     * <p>
     * <code><pre>
     * c major minor
     * m b16 b8 method-name
     * </pre></code>
     *
     * @param method the method name to call.
     */
    public void startCall(String method, int length)
            throws IOException {
        os.write('c');
        os.write(_version);
        os.write(0);

        os.write('m');
        int len = method.length();
        os.write(len >> 8);
        os.write(len);
        printString(method, 0, len);
    }

    /**
     * Writes the call tag.  This would be followed by the
     * headers and the method tag.
     * <p>
     * <code><pre>
     * c major minor
     * </pre></code>
     *
     * @param method the method name to call.
     */
    public void startCall()
            throws IOException {
        os.write('c');
        os.write(0);
        os.write(1);
    }

    /**
     * Writes the method tag.
     * <p>
     * <code><pre>
     * m b16 b8 method-name
     * </pre></code>
     *
     * @param method the method name to call.
     */
    public void writeMethod(String method)
            throws IOException {
        os.write('m');
        int len = method.length();
        os.write(len >> 8);
        os.write(len);
        printString(method, 0, len);
    }

    /**
     * Completes.
     * <p>
     * <code><pre>
     * z
     * </pre></code>
     */
    public void completeCall()
            throws IOException {
        os.write('z');
    }

    /**
     * Starts the reply
     * <p>
     * <p>A successful completion will have a single value:
     * <p>
     * <pre>
     * r
     * </pre>
     */
    public void startReply()
            throws IOException {
        os.write('r');
        os.write(1);
        os.write(0);
    }

    /**
     * Completes reading the reply
     * <p>
     * <p>A successful completion will have a single value:
     * <p>
     * <pre>
     * z
     * </pre>
     */
    public void completeReply()
            throws IOException {
        os.write('z');
    }

    /**
     * Writes a header name.  The header value must immediately follow.
     * <p>
     * <code><pre>
     * H b16 b8 foo <em>value</em>
     * </pre></code>
     */
    public void writeHeader(String name)
            throws IOException {
        int len = name.length();

        os.write('H');
        os.write(len >> 8);
        os.write(len);

        printString(name);
    }

    /**
     * Writes a fault.  The fault will be written
     * as a descriptive string followed by an object:
     * <p>
     * <code><pre>
     * f
     * &lt;string>code
     * &lt;string>the fault code
     * <p>
     * &lt;string>message
     * &lt;string>the fault mesage
     * <p>
     * &lt;string>detail
     * mt\x00\xnnjavax.ejb.FinderException
     *     ...
     * z
     * z
     * </pre></code>
     *
     * @param code the fault code, a three digit
     */
    public void writeFault(String code, String message, Object detail)
            throws IOException {
        os.write('f');
        writeString("code");
        writeString(code);

        writeString("message");
        writeString(message);

        if (detail != null) {
            writeString("detail");
            writeObject(detail);
        }
        os.write('z');
    }

    /**
     * Writes any object to the output stream.
     */
    public void writeObject(Object object)
            throws IOException {
        if (object == null) {
            writeNull();
            return;
        }

        Serializer serializer;

        serializer = _serializerFactory.getSerializer(object.getClass());

        serializer.writeObject(object, this);
    }

    /**
     * Writes the list header to the stream.  List writers will call
     * <code>writeListBegin</code> followed by the list contents and then
     * call <code>writeListEnd</code>.
     * <p>
     * <code><pre>
     * V
     * t b16 b8 type
     * l b32 b24 b16 b8
     * </pre></code>
     */
    public boolean writeListBegin(int length, String type)
            throws IOException {
        os.write('V');

        if (type != null) {
            os.write('t');
            printLenString(type);
        }

        if (length >= 0) {
            os.write('l');
            os.write(length >> 24);
            os.write(length >> 16);
            os.write(length >> 8);
            os.write(length);
        }

        return true;
    }

    /**
     * Writes the tail of the list to the stream.
     */
    public void writeListEnd()
            throws IOException {
        os.write('z');
    }

    /**
     * Writes the map header to the stream.  Map writers will call
     * <code>writeMapBegin</code> followed by the map contents and then
     * call <code>writeMapEnd</code>.
     * <p>
     * <code><pre>
     * Mt b16 b8 (<key> <value>)z
     * </pre></code>
     */
    public void writeMapBegin(String type)
            throws IOException {
        os.write('M');
        os.write('t');
        printLenString(type);
    }

    /**
     * Writes the tail of the map to the stream.
     */
    public void writeMapEnd()
            throws IOException {
        os.write('z');
    }

    /**
     * Writes a remote object reference to the stream.  The type is the
     * type of the remote interface.
     * <p>
     * <code><pre>
     * 'r' 't' b16 b8 type url
     * </pre></code>
     */
    public void writeRemote(String type, String url)
            throws IOException {
        os.write('r');
        os.write('t');
        printLenString(type);
        os.write('S');
        printLenString(url);
    }

    /**
     * Writes a boolean value to the stream.  The boolean will be written
     * with the following syntax:
     * <p>
     * <code><pre>
     * T
     * F
     * </pre></code>
     *
     * @param value the boolean value to write.
     */
    public void writeBoolean(boolean value)
            throws IOException {
        if (value)
            os.write('T');
        else
            os.write('F');
    }

    /**
     * Writes an integer value to the stream.  The integer will be written
     * with the following syntax:
     * <p>
     * <code><pre>
     * I b32 b24 b16 b8
     * </pre></code>
     *
     * @param value the integer value to write.
     */
    public void writeInt(int value)
            throws IOException {
        os.write('I');
        os.write(value >> 24);
        os.write(value >> 16);
        os.write(value >> 8);
        os.write(value);
    }

    /**
     * Writes a long value to the stream.  The long will be written
     * with the following syntax:
     * <p>
     * <code><pre>
     * L b64 b56 b48 b40 b32 b24 b16 b8
     * </pre></code>
     *
     * @param value the long value to write.
     */
    public void writeLong(long value)
            throws IOException {
        os.write('L');
        os.write((byte) (value >> 56));
        os.write((byte) (value >> 48));
        os.write((byte) (value >> 40));
        os.write((byte) (value >> 32));
        os.write((byte) (value >> 24));
        os.write((byte) (value >> 16));
        os.write((byte) (value >> 8));
        os.write((byte) (value));
    }

    /**
     * Writes a double value to the stream.  The double will be written
     * with the following syntax:
     * <p>
     * <code><pre>
     * D b64 b56 b48 b40 b32 b24 b16 b8
     * </pre></code>
     *
     * @param value the double value to write.
     */
    public void writeDouble(double value)
            throws IOException {
        long bits = Double.doubleToLongBits(value);

        os.write('D');
        os.write((byte) (bits >> 56));
        os.write((byte) (bits >> 48));
        os.write((byte) (bits >> 40));
        os.write((byte) (bits >> 32));
        os.write((byte) (bits >> 24));
        os.write((byte) (bits >> 16));
        os.write((byte) (bits >> 8));
        os.write((byte) (bits));
    }

    /**
     * Writes a date to the stream.
     * <p>
     * <code><pre>
     * T  b64 b56 b48 b40 b32 b24 b16 b8
     * </pre></code>
     *
     * @param time the date in milliseconds from the epoch in UTC
     */
    public void writeUTCDate(long time)
            throws IOException {
        os.write('d');
        os.write((byte) (time >> 56));
        os.write((byte) (time >> 48));
        os.write((byte) (time >> 40));
        os.write((byte) (time >> 32));
        os.write((byte) (time >> 24));
        os.write((byte) (time >> 16));
        os.write((byte) (time >> 8));
        os.write((byte) (time));
    }

    /**
     * Writes a null value to the stream.
     * The null will be written with the following syntax
     * <p>
     * <code><pre>
     * N
     * </pre></code>
     *
     * @param value the string value to write.
     */
    public void writeNull()
            throws IOException {
        os.write('N');
    }

    /**
     * Writes a string value to the stream using UTF-8 encoding.
     * The string will be written with the following syntax:
     * <p>
     * <code><pre>
     * S b16 b8 string-value
     * </pre></code>
     * <p>
     * If the value is null, it will be written as
     * <p>
     * <code><pre>
     * N
     * </pre></code>
     *
     * @param value the string value to write.
     */
    public void writeString(String value)
            throws IOException {
        if (value == null) {
            os.write('N');
        } else {
            int length = value.length();
            int offset = 0;

            while (length > 0x8000) {
                int sublen = 0x8000;

                // chunk can't end in high surrogate
                char tail = value.charAt(offset + sublen - 1);

                if (0xd800 <= tail && tail <= 0xdbff)
                    sublen--;

                os.write('s');
                os.write(sublen >> 8);
                os.write(sublen);

                printString(value, offset, sublen);

                length -= sublen;
                offset += sublen;
            }

            os.write('S');
            os.write(length >> 8);
            os.write(length);

            printString(value, offset, length);
        }
    }

    /**
     * Writes a string value to the stream using UTF-8 encoding.
     * The string will be written with the following syntax:
     * <p>
     * <code><pre>
     * S b16 b8 string-value
     * </pre></code>
     * <p>
     * If the value is null, it will be written as
     * <p>
     * <code><pre>
     * N
     * </pre></code>
     *
     * @param value the string value to write.
     */
    public void writeString(char[] buffer, int offset, int length)
            throws IOException {
        if (buffer == null) {
            os.write('N');
        } else {
            while (length > 0x8000) {
                int sublen = 0x8000;

                // chunk can't end in high surrogate
                char tail = buffer[offset + sublen - 1];

                if (0xd800 <= tail && tail <= 0xdbff)
                    sublen--;

                os.write('s');
                os.write(sublen >> 8);
                os.write(sublen);

                printString(buffer, offset, sublen);

                length -= sublen;
                offset += sublen;
            }

            os.write('S');
            os.write(length >> 8);
            os.write(length);

            printString(buffer, offset, length);
        }
    }

    /**
     * Writes a byte array to the stream.
     * The array will be written with the following syntax:
     * <p>
     * <code><pre>
     * B b16 b18 bytes
     * </pre></code>
     * <p>
     * If the value is null, it will be written as
     * <p>
     * <code><pre>
     * N
     * </pre></code>
     *
     * @param value the string value to write.
     */
    public void writeBytes(byte[] buffer)
            throws IOException {
        if (buffer == null)
            os.write('N');
        else
            writeBytes(buffer, 0, buffer.length);
    }

    /**
     * Writes a byte array to the stream.
     * The array will be written with the following syntax:
     * <p>
     * <code><pre>
     * B b16 b18 bytes
     * </pre></code>
     * <p>
     * If the value is null, it will be written as
     * <p>
     * <code><pre>
     * N
     * </pre></code>
     *
     * @param value the string value to write.
     */
    public void writeBytes(byte[] buffer, int offset, int length)
            throws IOException {
        if (buffer == null) {
            os.write('N');
        } else {
            while (length > 0x8000) {
                int sublen = 0x8000;

                os.write('b');
                os.write(sublen >> 8);
                os.write(sublen);

                os.write(buffer, offset, sublen);

                length -= sublen;
                offset += sublen;
            }

            os.write('B');
            os.write(length >> 8);
            os.write(length);
            os.write(buffer, offset, length);
        }
    }

    /**
     * Writes a byte buffer to the stream.
     * <p>
     * <code><pre>
     * </pre></code>
     */
    public void writeByteBufferStart()
            throws IOException {
    }

    /**
     * Writes a byte buffer to the stream.
     * <p>
     * <code><pre>
     * b b16 b18 bytes
     * </pre></code>
     */
    public void writeByteBufferPart(byte[] buffer, int offset, int length)
            throws IOException {
        while (length > 0) {
            int sublen = length;

            if (0x8000 < sublen)
                sublen = 0x8000;

            os.write('b');
            os.write(sublen >> 8);
            os.write(sublen);

            os.write(buffer, offset, sublen);

            length -= sublen;
            offset += sublen;
        }
    }

    /**
     * Writes a byte buffer to the stream.
     * <p>
     * <code><pre>
     * b b16 b18 bytes
     * </pre></code>
     */
    public void writeByteBufferEnd(byte[] buffer, int offset, int length)
            throws IOException {
        writeBytes(buffer, offset, length);
    }

    /**
     * Writes a reference.
     * <p>
     * <code><pre>
     * R b32 b24 b16 b8
     * </pre></code>
     *
     * @param value the integer value to write.
     */
    public void writeRef(int value)
            throws IOException {
        os.write('R');
        os.write(value >> 24);
        os.write(value >> 16);
        os.write(value >> 8);
        os.write(value);
    }

    /**
     * Writes a placeholder.
     * <p>
     * <code><pre>
     * P
     * </pre></code>
     */
    public void writePlaceholder()
            throws IOException {
        os.write('P');
    }

    /**
     * If the object has already been written, just write its ref.
     *
     * @return true if we're writing a ref.
     */
    public boolean addRef(Object object)
            throws IOException {
        if (_refs == null)
            _refs = new IdentityHashMap();

        Integer ref = (Integer) _refs.get(object);

        if (ref != null) {
            int value = ref.intValue();

            writeRef(value);
            return true;
        } else {
            _refs.put(object, new Integer(_refs.size()));

            return false;
        }
    }

    /**
     * Resets the references for streaming.
     */
    public void resetReferences() {
        if (_refs != null)
            _refs.clear();
    }

    /**
     * Removes a reference.
     */
    public boolean removeRef(Object obj)
            throws IOException {
        if (_refs != null) {
            _refs.remove(obj);

            return true;
        } else
            return false;
    }

    /**
     * Replaces a reference from one object to another.
     */
    public boolean replaceRef(Object oldRef, Object newRef)
            throws IOException {
        Integer value = (Integer) _refs.remove(oldRef);

        if (value != null) {
            _refs.put(newRef, value);
            return true;
        } else
            return false;
    }

    /**
     * Prints a string to the stream, encoded as UTF-8 with preceeding length
     *
     * @param v the string to print.
     */
    public void printLenString(String v)
            throws IOException {
        if (v == null) {
            os.write(0);
            os.write(0);
        } else {
            int len = v.length();
            os.write(len >> 8);
            os.write(len);

            printString(v, 0, len);
        }
    }

    /**
     * Prints a string to the stream, encoded as UTF-8
     *
     * @param v the string to print.
     */
    public void printString(String v)
            throws IOException {
        printString(v, 0, v.length());
    }

    /**
     * Prints a string to the stream, encoded as UTF-8
     *
     * @param v the string to print.
     */
    public void printString(String v, int offset, int length)
            throws IOException {
        for (int i = 0; i < length; i++) {
            char ch = v.charAt(i + offset);

            if (ch < 0x80)
                os.write(ch);
            else if (ch < 0x800) {
                os.write(0xc0 + ((ch >> 6) & 0x1f));
                os.write(0x80 + (ch & 0x3f));
            } else {
                os.write(0xe0 + ((ch >> 12) & 0xf));
                os.write(0x80 + ((ch >> 6) & 0x3f));
                os.write(0x80 + (ch & 0x3f));
            }
        }
    }

    /**
     * Prints a string to the stream, encoded as UTF-8
     *
     * @param v the string to print.
     */
    public void printString(char[] v, int offset, int length)
            throws IOException {
        for (int i = 0; i < length; i++) {
            char ch = v[i + offset];

            if (ch < 0x80)
                os.write(ch);
            else if (ch < 0x800) {
                os.write(0xc0 + ((ch >> 6) & 0x1f));
                os.write(0x80 + (ch & 0x3f));
            } else {
                os.write(0xe0 + ((ch >> 12) & 0xf));
                os.write(0x80 + ((ch >> 6) & 0x3f));
                os.write(0x80 + (ch & 0x3f));
            }
        }
    }

    public void flush()
            throws IOException {
        if (this.os != null)
            this.os.flush();
    }

    public void close()
            throws IOException {
        if (this.os != null)
            this.os.flush();
    }
}
