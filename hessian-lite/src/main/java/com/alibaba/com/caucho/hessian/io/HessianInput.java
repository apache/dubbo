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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Input stream for Hessian requests.
 * <p>
 * <p>HessianInput is unbuffered, so any client needs to provide
 * its own buffering.
 * <p>
 * <pre>
 * InputStream is = ...; // from http connection
 * HessianInput in = new HessianInput(is);
 * String value;
 *
 * in.startReply();         // read reply header
 * value = in.readString(); // read string value
 * in.completeReply();      // read reply footer
 * </pre>
 */
public class HessianInput extends AbstractHessianInput {
    private static int END_OF_DATA = -2;

    private static Field _detailMessageField;

    static {
        try {
            _detailMessageField = Throwable.class.getDeclaredField("detailMessage");
            _detailMessageField.setAccessible(true);
        } catch (Throwable e) {
        }
    }

    // factory for deserializing objects in the input stream
    protected SerializerFactory _serializerFactory;
    protected ArrayList _refs;
    // a peek character
    protected int _peek = -1;
    // the underlying input stream
    private InputStream _is;
    // the method for a call
    private String _method;
    private Reader _chunkReader;
    private InputStream _chunkInputStream;
    private Throwable _replyFault;
    private StringBuffer _sbuf = new StringBuffer();
    // true if this is the last chunk
    private boolean _isLastChunk;
    // the chunk length
    private int _chunkLength;

    /**
     * Creates an uninitialized Hessian input stream.
     */
    public HessianInput() {
    }

    /**
     * Creates a new Hessian input stream, initialized with an
     * underlying input stream.
     *
     * @param is the underlying input stream.
     */
    public HessianInput(InputStream is) {
        init(is);
    }

    /**
     * Gets the serializer factory.
     */
    public SerializerFactory getSerializerFactory() {
        return _serializerFactory;
    }

    /**
     * Sets the serializer factory.
     */
    @Override
    public void setSerializerFactory(SerializerFactory factory) {
        _serializerFactory = factory;
    }

    /**
     * Initialize the hessian stream with the underlying input stream.
     */
    @Override
    public void init(InputStream is) {
        _is = is;
        _method = null;
        _isLastChunk = true;
        _chunkLength = 0;
        _peek = -1;
        _refs = null;
        _replyFault = null;

        if (_serializerFactory == null)
            _serializerFactory = new SerializerFactory();
    }

    /**
     * Returns the calls method
     */
    @Override
    public String getMethod() {
        return _method;
    }

    /**
     * Returns any reply fault.
     */
    public Throwable getReplyFault() {
        return _replyFault;
    }

    /**
     * Starts reading the call
     * <p>
     * <pre>
     * c major minor
     * </pre>
     */
    @Override
    public int readCall()
            throws IOException {
        int tag = read();

        if (tag != 'c')
            throw error("expected hessian call ('c') at " + codeName(tag));

        int major = read();
        int minor = read();

        return (major << 16) + minor;
    }

    /**
     * For backward compatibility with HessianSkeleton
     */
    @Override
    public void skipOptionalCall()
            throws IOException {
        int tag = read();

        if (tag == 'c') {
            read();
            read();
        } else
            _peek = tag;
    }

    /**
     * Starts reading the call
     * <p>
     * <p>A successful completion will have a single value:
     * <p>
     * <pre>
     * m b16 b8 method
     * </pre>
     */
    @Override
    public String readMethod()
            throws IOException {
        int tag = read();

        if (tag != 'm')
            throw error("expected hessian method ('m') at " + codeName(tag));
        int d1 = read();
        int d2 = read();

        _isLastChunk = true;
        _chunkLength = d1 * 256 + d2;
        _sbuf.setLength(0);
        int ch;
        while ((ch = parseChar()) >= 0)
            _sbuf.append((char) ch);

        _method = _sbuf.toString();

        return _method;
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
    @Override
    public void startCall()
            throws IOException {
        readCall();

        while (readHeader() != null) {
            readObject();
        }

        readMethod();
    }

    /**
     * Completes reading the call
     * <p>
     * <p>A successful completion will have a single value:
     * <p>
     * <pre>
     * z
     * </pre>
     */
    @Override
    public void completeCall()
            throws IOException {
        int tag = read();

        if (tag == 'z') {
        } else
            throw error("expected end of call ('z') at " + codeName(tag) + ".  Check method arguments and ensure method overloading is enabled if necessary");
    }

    /**
     * Reads a reply as an object.
     * If the reply has a fault, throws the exception.
     */
    @Override
    public Object readReply(Class expectedClass)
            throws Throwable {
        int tag = read();

        if (tag != 'r')
            error("expected hessian reply at " + codeName(tag));

        int major = read();
        int minor = read();

        tag = read();
        if (tag == 'f')
            throw prepareFault();
        else {
            _peek = tag;

            Object value = readObject(expectedClass);

            completeValueReply();

            return value;
        }
    }

    /**
     * Starts reading the reply
     * <p>
     * <p>A successful completion will have a single value:
     * <p>
     * <pre>
     * r
     * </pre>
     */
    @Override
    public void startReply()
            throws Throwable {
        int tag = read();

        if (tag != 'r')
            error("expected hessian reply at " + codeName(tag));

        int major = read();
        int minor = read();

        tag = read();
        if (tag == 'f')
            throw prepareFault();
        else
            _peek = tag;
    }

    /**
     * Prepares the fault.
     */
    private Throwable prepareFault()
            throws IOException {
        HashMap fault = readFault();

        Object detail = fault.get("detail");
        String message = (String) fault.get("message");

        if (detail instanceof Throwable) {
            _replyFault = (Throwable) detail;

            if (message != null && _detailMessageField != null) {
                try {
                    _detailMessageField.set(_replyFault, message);
                } catch (Throwable e) {
                }
            }

            return _replyFault;
        } else {
            String code = (String) fault.get("code");

            _replyFault = new HessianServiceException(message, code, detail);

            return _replyFault;
        }
    }

    /**
     * Completes reading the call
     * <p>
     * <p>A successful completion will have a single value:
     * <p>
     * <pre>
     * z
     * </pre>
     */
    @Override
    public void completeReply()
            throws IOException {
        int tag = read();

        if (tag != 'z')
            error("expected end of reply at " + codeName(tag));
    }

    /**
     * Completes reading the call
     * <p>
     * <p>A successful completion will have a single value:
     * <p>
     * <pre>
     * z
     * </pre>
     */
    public void completeValueReply()
            throws IOException {
        int tag = read();

        if (tag != 'z')
            error("expected end of reply at " + codeName(tag));
    }

    /**
     * Reads a header, returning null if there are no headers.
     * <p>
     * <pre>
     * H b16 b8 value
     * </pre>
     */
    @Override
    public String readHeader()
            throws IOException {
        int tag = read();

        if (tag == 'H') {
            _isLastChunk = true;
            _chunkLength = (read() << 8) + read();

            _sbuf.setLength(0);
            int ch;
            while ((ch = parseChar()) >= 0)
                _sbuf.append((char) ch);

            return _sbuf.toString();
        }

        _peek = tag;

        return null;
    }

    /**
     * Reads a null
     * <p>
     * <pre>
     * N
     * </pre>
     */
    @Override
    public void readNull()
            throws IOException {
        int tag = read();

        switch (tag) {
            case 'N':
                return;

            default:
                throw expect("null", tag);
        }
    }

    /**
     * Reads a byte
     *
     * <pre>
     * I b32 b24 b16 b8
     * </pre>
     */
  /*
  public byte readByte()
    throws IOException
  {
    return (byte) readInt();
  }
  */

    /**
     * Reads a boolean
     * <p>
     * <pre>
     * T
     * F
     * </pre>
     */
    @Override
    public boolean readBoolean()
            throws IOException {
        int tag = read();

        switch (tag) {
            case 'T':
                return true;
            case 'F':
                return false;
            case 'I':
                return parseInt() == 0;
            case 'L':
                return parseLong() == 0;
            case 'D':
                return parseDouble() == 0.0;
            case 'N':
                return false;

            default:
                throw expect("boolean", tag);
        }
    }

    /**
     * Reads a short
     * <p>
     * <pre>
     * I b32 b24 b16 b8
     * </pre>
     */
    public short readShort()
            throws IOException {
        return (short) readInt();
    }

    /**
     * Reads an integer
     * <p>
     * <pre>
     * I b32 b24 b16 b8
     * </pre>
     */
    @Override
    public int readInt()
            throws IOException {
        int tag = read();

        switch (tag) {
            case 'T':
                return 1;
            case 'F':
                return 0;
            case 'I':
                return parseInt();
            case 'L':
                return (int) parseLong();
            case 'D':
                return (int) parseDouble();

            default:
                throw expect("int", tag);
        }
    }

    /**
     * Reads a long
     * <p>
     * <pre>
     * L b64 b56 b48 b40 b32 b24 b16 b8
     * </pre>
     */
    @Override
    public long readLong()
            throws IOException {
        int tag = read();

        switch (tag) {
            case 'T':
                return 1;
            case 'F':
                return 0;
            case 'I':
                return parseInt();
            case 'L':
                return parseLong();
            case 'D':
                return (long) parseDouble();

            default:
                throw expect("long", tag);
        }
    }

    /**
     * Reads a float
     * <p>
     * <pre>
     * D b64 b56 b48 b40 b32 b24 b16 b8
     * </pre>
     */
    public float readFloat()
            throws IOException {
        return (float) readDouble();
    }

    /**
     * Reads a double
     * <p>
     * <pre>
     * D b64 b56 b48 b40 b32 b24 b16 b8
     * </pre>
     */
    @Override
    public double readDouble()
            throws IOException {
        int tag = read();

        switch (tag) {
            case 'T':
                return 1;
            case 'F':
                return 0;
            case 'I':
                return parseInt();
            case 'L':
                return (double) parseLong();
            case 'D':
                return parseDouble();

            default:
                throw expect("long", tag);
        }
    }

    /**
     * Reads a date.
     * <p>
     * <pre>
     * T b64 b56 b48 b40 b32 b24 b16 b8
     * </pre>
     */
    @Override
    public long readUTCDate()
            throws IOException {
        int tag = read();

        if (tag != 'd')
            throw error("expected date at " + codeName(tag));

        long b64 = read();
        long b56 = read();
        long b48 = read();
        long b40 = read();
        long b32 = read();
        long b24 = read();
        long b16 = read();
        long b8 = read();

        return ((b64 << 56) +
                (b56 << 48) +
                (b48 << 40) +
                (b40 << 32) +
                (b32 << 24) +
                (b24 << 16) +
                (b16 << 8) +
                b8);
    }

    /**
     * Reads a byte from the stream.
     */
    public int readChar()
            throws IOException {
        if (_chunkLength > 0) {
            _chunkLength--;
            if (_chunkLength == 0 && _isLastChunk)
                _chunkLength = END_OF_DATA;

            int ch = parseUTF8Char();
            return ch;
        } else if (_chunkLength == END_OF_DATA) {
            _chunkLength = 0;
            return -1;
        }

        int tag = read();

        switch (tag) {
            case 'N':
                return -1;

            case 'S':
            case 's':
            case 'X':
            case 'x':
                _isLastChunk = tag == 'S' || tag == 'X';
                _chunkLength = (read() << 8) + read();

                _chunkLength--;
                int value = parseUTF8Char();

                // special code so successive read byte won't
                // be read as a single object.
                if (_chunkLength == 0 && _isLastChunk)
                    _chunkLength = END_OF_DATA;

                return value;

            default:
                throw new IOException("expected 'S' at " + (char) tag);
        }
    }

    /**
     * Reads a byte array from the stream.
     */
    public int readString(char[] buffer, int offset, int length)
            throws IOException {
        int readLength = 0;

        if (_chunkLength == END_OF_DATA) {
            _chunkLength = 0;
            return -1;
        } else if (_chunkLength == 0) {
            int tag = read();

            switch (tag) {
                case 'N':
                    return -1;

                case 'S':
                case 's':
                case 'X':
                case 'x':
                    _isLastChunk = tag == 'S' || tag == 'X';
                    _chunkLength = (read() << 8) + read();
                    break;

                default:
                    throw new IOException("expected 'S' at " + (char) tag);
            }
        }

        while (length > 0) {
            if (_chunkLength > 0) {
                buffer[offset++] = (char) parseUTF8Char();
                _chunkLength--;
                length--;
                readLength++;
            } else if (_isLastChunk) {
                if (readLength == 0)
                    return -1;
                else {
                    _chunkLength = END_OF_DATA;
                    return readLength;
                }
            } else {
                int tag = read();

                switch (tag) {
                    case 'S':
                    case 's':
                    case 'X':
                    case 'x':
                        _isLastChunk = tag == 'S' || tag == 'X';
                        _chunkLength = (read() << 8) + read();
                        break;

                    default:
                        throw new IOException("expected 'S' at " + (char) tag);
                }
            }
        }

        if (readLength == 0)
            return -1;
        else if (_chunkLength > 0 || !_isLastChunk)
            return readLength;
        else {
            _chunkLength = END_OF_DATA;
            return readLength;
        }
    }

    /**
     * Reads a string
     * <p>
     * <pre>
     * S b16 b8 string value
     * </pre>
     */
    @Override
    public String readString()
            throws IOException {
        int tag = read();

        switch (tag) {
            case 'N':
                return null;

            case 'I':
                return String.valueOf(parseInt());
            case 'L':
                return String.valueOf(parseLong());
            case 'D':
                return String.valueOf(parseDouble());

            case 'S':
            case 's':
            case 'X':
            case 'x':
                _isLastChunk = tag == 'S' || tag == 'X';
                _chunkLength = (read() << 8) + read();

                _sbuf.setLength(0);
                int ch;

                while ((ch = parseChar()) >= 0)
                    _sbuf.append((char) ch);

                return _sbuf.toString();

            default:
                throw expect("string", tag);
        }
    }

    /**
     * Reads an XML node.
     * <p>
     * <pre>
     * S b16 b8 string value
     * </pre>
     */
    @Override
    public org.w3c.dom.Node readNode()
            throws IOException {
        int tag = read();

        switch (tag) {
            case 'N':
                return null;

            case 'S':
            case 's':
            case 'X':
            case 'x':
                _isLastChunk = tag == 'S' || tag == 'X';
                _chunkLength = (read() << 8) + read();

                throw error("Can't handle string in this context");

            default:
                throw expect("string", tag);
        }
    }

    /**
     * Reads a byte array
     * <p>
     * <pre>
     * B b16 b8 data value
     * </pre>
     */
    @Override
    public byte[] readBytes()
            throws IOException {
        int tag = read();

        switch (tag) {
            case 'N':
                return null;

            case 'B':
            case 'b':
                _isLastChunk = tag == 'B';
                _chunkLength = (read() << 8) + read();

                ByteArrayOutputStream bos = new ByteArrayOutputStream();

                int data;
                while ((data = parseByte()) >= 0)
                    bos.write(data);

                return bos.toByteArray();

            default:
                throw expect("bytes", tag);
        }
    }

    /**
     * Reads a byte from the stream.
     */
    public int readByte()
            throws IOException {
        if (_chunkLength > 0) {
            _chunkLength--;
            if (_chunkLength == 0 && _isLastChunk)
                _chunkLength = END_OF_DATA;

            return read();
        } else if (_chunkLength == END_OF_DATA) {
            _chunkLength = 0;
            return -1;
        }

        int tag = read();

        switch (tag) {
            case 'N':
                return -1;

            case 'B':
            case 'b':
                _isLastChunk = tag == 'B';
                _chunkLength = (read() << 8) + read();

                int value = parseByte();

                // special code so successive read byte won't
                // be read as a single object.
                if (_chunkLength == 0 && _isLastChunk)
                    _chunkLength = END_OF_DATA;

                return value;

            default:
                throw new IOException("expected 'B' at " + (char) tag);
        }
    }

    /**
     * Reads a byte array from the stream.
     */
    public int readBytes(byte[] buffer, int offset, int length)
            throws IOException {
        int readLength = 0;

        if (_chunkLength == END_OF_DATA) {
            _chunkLength = 0;
            return -1;
        } else if (_chunkLength == 0) {
            int tag = read();

            switch (tag) {
                case 'N':
                    return -1;

                case 'B':
                case 'b':
                    _isLastChunk = tag == 'B';
                    _chunkLength = (read() << 8) + read();
                    break;

                default:
                    throw new IOException("expected 'B' at " + (char) tag);
            }
        }

        while (length > 0) {
            if (_chunkLength > 0) {
                buffer[offset++] = (byte) read();
                _chunkLength--;
                length--;
                readLength++;
            } else if (_isLastChunk) {
                if (readLength == 0)
                    return -1;
                else {
                    _chunkLength = END_OF_DATA;
                    return readLength;
                }
            } else {
                int tag = read();

                switch (tag) {
                    case 'B':
                    case 'b':
                        _isLastChunk = tag == 'B';
                        _chunkLength = (read() << 8) + read();
                        break;

                    default:
                        throw new IOException("expected 'B' at " + (char) tag);
                }
            }
        }

        if (readLength == 0)
            return -1;
        else if (_chunkLength > 0 || !_isLastChunk)
            return readLength;
        else {
            _chunkLength = END_OF_DATA;
            return readLength;
        }
    }

    /**
     * Reads a fault.
     */
    private HashMap readFault()
            throws IOException {
        HashMap map = new HashMap();

        int code = read();
        for (; code > 0 && code != 'z'; code = read()) {
            _peek = code;

            Object key = readObject();
            Object value = readObject();

            if (key != null && value != null)
                map.put(key, value);
        }

        if (code != 'z')
            throw expect("fault", code);

        return map;
    }

    /**
     * Reads an object from the input stream with an expected type.
     */
    @Override
    public Object readObject(Class cl)
            throws IOException {
        return readObject(cl, null, null);
    }

    /**
     * Reads an object from the input stream with an expected type.
     */
    public Object readObject(Class expectedClass, Class<?>... expectedTypes)
            throws IOException {
        if (expectedClass == null || expectedClass == Object.class)
            return readObject();

        int tag = read();

        switch (tag) {
            case 'N':
                return null;

            case 'M': {
                String type = readType();

                boolean keyValuePair = expectedTypes != null && expectedTypes.length == 2;

                // hessian/3386
                if ("".equals(type)) {
                    Deserializer reader;
                    reader = _serializerFactory.getDeserializer(expectedClass);

                    return reader.readMap(this
                            , keyValuePair ? expectedTypes[0] : null
                            , keyValuePair ? expectedTypes[1] : null);
                } else {
                    Deserializer reader;
                    reader = _serializerFactory.getObjectDeserializer(type, expectedClass);

                    return reader.readMap(this
                            , keyValuePair ? expectedTypes[0] : null
                            , keyValuePair ? expectedTypes[1] : null);
                }
            }

            case 'V': {
                String type = readType();
                int length = readLength();

                Deserializer reader;
                reader = _serializerFactory.getObjectDeserializer(type);

                boolean valueType = expectedTypes != null && expectedTypes.length == 1;

                if (expectedClass != reader.getType() && expectedClass.isAssignableFrom(reader.getType()))
                    return reader.readList(this, length, valueType ? expectedTypes[0] : null);

                reader = _serializerFactory.getDeserializer(expectedClass);

                Object v = reader.readList(this, length, valueType ? expectedTypes[0] : null);

                return v;
            }

            case 'R': {
                int ref = parseInt();

                return _refs.get(ref);
            }

            case 'r': {
                String type = readType();
                String url = readString();

                return resolveRemote(type, url);
            }
        }

        _peek = tag;

        // hessian/332i vs hessian/3406
        //return readObject();

        Object value = _serializerFactory.getDeserializer(expectedClass).readObject(this);

        return value;
    }

    /**
     * Reads an arbitrary object from the input stream when the type
     * is unknown.
     */
    @Override
    public Object readObject()
            throws IOException {
        return readObject((List<Class<?>>) null);
    }

    /**
     * Reads an arbitrary object from the input stream when the type
     * is unknown.
     */
    public Object readObject(List<Class<?>> expectedTypes)
            throws IOException {
        int tag = read();

        switch (tag) {
            case 'N':
                return null;

            case 'T':
                return Boolean.valueOf(true);

            case 'F':
                return Boolean.valueOf(false);

            case 'I':
                return Integer.valueOf(parseInt());

            case 'L':
                return Long.valueOf(parseLong());

            case 'D':
                return Double.valueOf(parseDouble());

            case 'd':
                return new Date(parseLong());

            case 'x':
            case 'X': {
                _isLastChunk = tag == 'X';
                _chunkLength = (read() << 8) + read();

                return parseXML();
            }

            case 's':
            case 'S': {
                _isLastChunk = tag == 'S';
                _chunkLength = (read() << 8) + read();

                int data;
                _sbuf.setLength(0);

                while ((data = parseChar()) >= 0)
                    _sbuf.append((char) data);

                return _sbuf.toString();
            }

            case 'b':
            case 'B': {
                _isLastChunk = tag == 'B';
                _chunkLength = (read() << 8) + read();

                int data;
                ByteArrayOutputStream bos = new ByteArrayOutputStream();

                while ((data = parseByte()) >= 0)
                    bos.write(data);

                return bos.toByteArray();
            }

            case 'V': {
                String type = readType();
                int length = readLength();

                Deserializer reader;
                reader = _serializerFactory.getObjectDeserializer(type);

                boolean valueType = expectedTypes != null && expectedTypes.size() == 1;

                if (List.class != reader.getType() && List.class.isAssignableFrom(reader.getType()))
                    return reader.readList(this, length, valueType ? expectedTypes.get(0) : null);

                reader = _serializerFactory.getDeserializer(List.class);

                Object v = reader.readList(this, length, valueType ? expectedTypes.get(0) : null);

                return v;
            }

            case 'M': {
                String type = readType();

                boolean keyValuePair = expectedTypes != null && expectedTypes.size() == 2;

                return _serializerFactory.readMap(this, type
                        , keyValuePair ? expectedTypes.get(0) : null
                        , keyValuePair ? expectedTypes.get(1) : null);
            }

            case 'R': {
                int ref = parseInt();

                return _refs.get(ref);
            }

            case 'r': {
                String type = readType();
                String url = readString();

                return resolveRemote(type, url);
            }

            default:
                throw error("unknown code for readObject at " + codeName(tag));
        }
    }

    /**
     * Reads a remote object.
     */
    @Override
    public Object readRemote()
            throws IOException {
        String type = readType();
        String url = readString();

        return resolveRemote(type, url);
    }

    /**
     * Reads a reference.
     */
    @Override
    public Object readRef()
            throws IOException {
        return _refs.get(parseInt());
    }

    /**
     * Reads the start of a list.
     */
    @Override
    public int readListStart()
            throws IOException {
        return read();
    }

    /**
     * Reads the start of a list.
     */
    @Override
    public int readMapStart()
            throws IOException {
        return read();
    }

    /**
     * Returns true if this is the end of a list or a map.
     */
    @Override
    public boolean isEnd()
            throws IOException {
        int code = read();

        _peek = code;

        return (code < 0 || code == 'z');
    }

    /**
     * Reads the end byte.
     */
    @Override
    public void readEnd()
            throws IOException {
        int code = read();

        if (code != 'z')
            throw error("unknown code at " + codeName(code));
    }

    /**
     * Reads the end byte.
     */
    @Override
    public void readMapEnd()
            throws IOException {
        int code = read();

        if (code != 'z')
            throw error("expected end of map ('z') at " + codeName(code));
    }

    /**
     * Reads the end byte.
     */
    @Override
    public void readListEnd()
            throws IOException {
        int code = read();

        if (code != 'z')
            throw error("expected end of list ('z') at " + codeName(code));
    }

    /**
     * Adds a list/map reference.
     */
    @Override
    public int addRef(Object ref) {
        if (_refs == null)
            _refs = new ArrayList();

        _refs.add(ref);

        return _refs.size() - 1;
    }

    /**
     * Adds a list/map reference.
     */
    @Override
    public void setRef(int i, Object ref) {
        _refs.set(i, ref);
    }

    /**
     * Resets the references for streaming.
     */
    @Override
    public void resetReferences() {
        if (_refs != null)
            _refs.clear();
    }

    /**
     * Resolves a remote object.
     */
    public Object resolveRemote(String type, String url)
            throws IOException {
        HessianRemoteResolver resolver = getRemoteResolver();

        if (resolver != null)
            return resolver.lookup(type, url);
        else
            return new HessianRemote(type, url);
    }

    /**
     * Parses a type from the stream.
     * <p>
     * <pre>
     * t b16 b8
     * </pre>
     */
    @Override
    public String readType()
            throws IOException {
        int code = read();

        if (code != 't') {
            _peek = code;
            return "";
        }

        _isLastChunk = true;
        _chunkLength = (read() << 8) + read();

        _sbuf.setLength(0);
        int ch;
        while ((ch = parseChar()) >= 0)
            _sbuf.append((char) ch);

        return _sbuf.toString();
    }

    /**
     * Parses the length for an array
     * <p>
     * <pre>
     * l b32 b24 b16 b8
     * </pre>
     */
    @Override
    public int readLength()
            throws IOException {
        int code = read();

        if (code != 'l') {
            _peek = code;
            return -1;
        }

        return parseInt();
    }

    /**
     * Parses a 32-bit integer value from the stream.
     * <p>
     * <pre>
     * b32 b24 b16 b8
     * </pre>
     */
    private int parseInt()
            throws IOException {
        int b32 = read();
        int b24 = read();
        int b16 = read();
        int b8 = read();

        return (b32 << 24) + (b24 << 16) + (b16 << 8) + b8;
    }

    /**
     * Parses a 64-bit long value from the stream.
     * <p>
     * <pre>
     * b64 b56 b48 b40 b32 b24 b16 b8
     * </pre>
     */
    private long parseLong()
            throws IOException {
        long b64 = read();
        long b56 = read();
        long b48 = read();
        long b40 = read();
        long b32 = read();
        long b24 = read();
        long b16 = read();
        long b8 = read();

        return ((b64 << 56) +
                (b56 << 48) +
                (b48 << 40) +
                (b40 << 32) +
                (b32 << 24) +
                (b24 << 16) +
                (b16 << 8) +
                b8);
    }

    /**
     * Parses a 64-bit double value from the stream.
     * <p>
     * <pre>
     * b64 b56 b48 b40 b32 b24 b16 b8
     * </pre>
     */
    private double parseDouble()
            throws IOException {
        long b64 = read();
        long b56 = read();
        long b48 = read();
        long b40 = read();
        long b32 = read();
        long b24 = read();
        long b16 = read();
        long b8 = read();

        long bits = ((b64 << 56) +
                (b56 << 48) +
                (b48 << 40) +
                (b40 << 32) +
                (b32 << 24) +
                (b24 << 16) +
                (b16 << 8) +
                b8);

        return Double.longBitsToDouble(bits);
    }

    org.w3c.dom.Node parseXML()
            throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Reads a character from the underlying stream.
     */
    private int parseChar()
            throws IOException {
        while (_chunkLength <= 0) {
            if (_isLastChunk)
                return -1;

            int code = read();

            switch (code) {
                case 's':
                case 'x':
                    _isLastChunk = false;

                    _chunkLength = (read() << 8) + read();
                    break;

                case 'S':
                case 'X':
                    _isLastChunk = true;

                    _chunkLength = (read() << 8) + read();
                    break;

                default:
                    throw expect("string", code);
            }

        }

        _chunkLength--;

        return parseUTF8Char();
    }

    /**
     * Parses a single UTF8 character.
     */
    private int parseUTF8Char()
            throws IOException {
        int ch = read();

        if (ch < 0x80)
            return ch;
        else if ((ch & 0xe0) == 0xc0) {
            int ch1 = read();
            int v = ((ch & 0x1f) << 6) + (ch1 & 0x3f);

            return v;
        } else if ((ch & 0xf0) == 0xe0) {
            int ch1 = read();
            int ch2 = read();
            int v = ((ch & 0x0f) << 12) + ((ch1 & 0x3f) << 6) + (ch2 & 0x3f);

            return v;
        } else
            throw error("bad utf-8 encoding at " + codeName(ch));
    }

    /**
     * Reads a byte from the underlying stream.
     */
    private int parseByte()
            throws IOException {
        while (_chunkLength <= 0) {
            if (_isLastChunk) {
                return -1;
            }

            int code = read();

            switch (code) {
                case 'b':
                    _isLastChunk = false;

                    _chunkLength = (read() << 8) + read();
                    break;

                case 'B':
                    _isLastChunk = true;

                    _chunkLength = (read() << 8) + read();
                    break;

                default:
                    throw expect("byte[]", code);
            }
        }

        _chunkLength--;

        return read();
    }

    /**
     * Reads bytes based on an input stream.
     */
    @Override
    public InputStream readInputStream()
            throws IOException {
        int tag = read();

        switch (tag) {
            case 'N':
                return null;

            case 'B':
            case 'b':
                _isLastChunk = tag == 'B';
                _chunkLength = (read() << 8) + read();
                break;

            default:
                throw expect("inputStream", tag);
        }

        return new InputStream() {
            boolean _isClosed = false;

            @Override
            public int read()
                    throws IOException {
                if (_isClosed || _is == null)
                    return -1;

                int ch = parseByte();
                if (ch < 0)
                    _isClosed = true;

                return ch;
            }

            @Override
            public int read(byte[] buffer, int offset, int length)
                    throws IOException {
                if (_isClosed || _is == null)
                    return -1;

                int len = HessianInput.this.read(buffer, offset, length);
                if (len < 0)
                    _isClosed = true;

                return len;
            }

            @Override
            public void close()
                    throws IOException {
                while (read() >= 0) {
                }

                _isClosed = true;
            }
        };
    }

    /**
     * Reads bytes from the underlying stream.
     */
    int read(byte[] buffer, int offset, int length)
            throws IOException {
        int readLength = 0;

        while (length > 0) {
            while (_chunkLength <= 0) {
                if (_isLastChunk)
                    return readLength == 0 ? -1 : readLength;

                int code = read();

                switch (code) {
                    case 'b':
                        _isLastChunk = false;

                        _chunkLength = (read() << 8) + read();
                        break;

                    case 'B':
                        _isLastChunk = true;

                        _chunkLength = (read() << 8) + read();
                        break;

                    default:
                        throw expect("byte[]", code);
                }
            }

            int sublen = _chunkLength;
            if (length < sublen)
                sublen = length;

            sublen = _is.read(buffer, offset, sublen);
            offset += sublen;
            readLength += sublen;
            length -= sublen;
            _chunkLength -= sublen;
        }

        return readLength;
    }

    final int read()
            throws IOException {
        if (_peek >= 0) {
            int value = _peek;
            _peek = -1;
            return value;
        }

        int ch = _is.read();

        return ch;
    }

    @Override
    public void close() {
        _is = null;
    }

    @Override
    public Reader getReader() {
        return null;
    }

    protected IOException expect(String expect, int ch) {
        return error("expected " + expect + " at " + codeName(ch));
    }

    protected String codeName(int ch) {
        if (ch < 0)
            return "end of file";
        else
            return "0x" + Integer.toHexString(ch & 0xff) + " (" + (char) +ch + ")";
    }

    protected IOException error(String message) {
        if (_method != null)
            return new HessianProtocolException(_method + ": " + message);
        else
            return new HessianProtocolException(message);
    }
}
