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
package com.alibaba.dubbo.remoting.transport.netty4;

import com.alibaba.dubbo.common.utils.Assert;
import com.alibaba.dubbo.remoting.buffer.ChannelBuffer;
import com.alibaba.dubbo.remoting.buffer.ChannelBufferFactory;
import com.alibaba.dubbo.remoting.buffer.ChannelBuffers;
import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class NettyBackedChannelBuffer implements ChannelBuffer {

    private ByteBuf buffer;

    public NettyBackedChannelBuffer(ByteBuf buffer) {
        Assert.notNull(buffer, "buffer == null");
        this.buffer = buffer;
    }

    
    public int capacity() {
        return buffer.capacity();
    }

    
    public ChannelBuffer copy(int index, int length) {
        return new NettyBackedChannelBuffer(buffer.copy(index, length));
    }

    //has nothing use
    public ChannelBufferFactory factory() {
        return null;
    }

    
    public byte getByte(int index) {
        return buffer.getByte(index);
    }

    
    public void getBytes(int index, byte[] dst, int dstIndex, int length) {
        buffer.getBytes(index, dst, dstIndex, length);
    }

    
    public void getBytes(int index, ByteBuffer dst) {
        buffer.getBytes(index, dst);
    }

    
    public void getBytes(int index, ChannelBuffer dst, int dstIndex, int length) {
        // careful
        byte[] data = new byte[length];
        buffer.getBytes(index, data, 0, length);
        dst.setBytes(dstIndex, data, 0, length);
    }

    
    public void getBytes(int index, OutputStream dst, int length) throws IOException {
        buffer.getBytes(index, dst, length);
    }

    
    public boolean isDirect() {
        return buffer.isDirect();
    }

    
    public void setByte(int index, int value) {
        buffer.setByte(index, value);
    }

    
    public void setBytes(int index, byte[] src, int srcIndex, int length) {
        buffer.setBytes(index, src, srcIndex, length);
    }

    
    public void setBytes(int index, ByteBuffer src) {
        buffer.setBytes(index, src);
    }

    
    public void setBytes(int index, ChannelBuffer src, int srcIndex, int length) {
        // careful
        byte[] data = new byte[length];
        buffer.getBytes(srcIndex, data, 0, length);
        setBytes(0, data, index, length);
    }

    
    public int setBytes(int index, InputStream src, int length) throws IOException {
        return buffer.setBytes(index, src, length);
    }

    
    public ByteBuffer toByteBuffer(int index, int length) {
        return buffer.nioBuffer(index, length);
    }

    
    public byte[] array() {
        return buffer.array();
    }

    
    public boolean hasArray() {
        return buffer.hasArray();
    }

    
    public int arrayOffset() {
        return buffer.arrayOffset();
    }


    // AbstractChannelBuffer


    
    public void clear() {
        buffer.clear();
    }

    
    public ChannelBuffer copy() {
        return new NettyBackedChannelBuffer(buffer.copy());
    }

    
    public void discardReadBytes() {
        buffer.discardReadBytes();
    }

    
    public void ensureWritableBytes(int writableBytes) {
        buffer.ensureWritable(writableBytes);
    }

    
    public void getBytes(int index, byte[] dst) {
        buffer.getBytes(index, dst);
    }

    
    public void getBytes(int index, ChannelBuffer dst) {
        // careful
        getBytes(index, dst, dst.writableBytes());
    }

    
    public void getBytes(int index, ChannelBuffer dst, int length) {
        // careful
        if (length > dst.writableBytes()) {
            throw new IndexOutOfBoundsException();
        }
        getBytes(index, dst, dst.writerIndex(), length);
        dst.writerIndex(dst.writerIndex() + length);
    }

    
    public void markReaderIndex() {
        buffer.markReaderIndex();
    }

    
    public void markWriterIndex() {
        buffer.markWriterIndex();
    }

    
    public boolean readable() {
        return buffer.isReadable();
    }

    
    public int readableBytes() {
        return buffer.readableBytes();
    }

    
    public byte readByte() {
        return buffer.readByte();
    }

    
    public void readBytes(byte[] dst) {
        buffer.readBytes(dst);
    }

    
    public void readBytes(byte[] dst, int dstIndex, int length) {
        buffer.readBytes(dst, dstIndex, length);
    }

    
    public void readBytes(ByteBuffer dst) {
        buffer.readBytes(dst);
    }

    
    public void readBytes(ChannelBuffer dst) {
        // careful
        readBytes(dst, dst.writableBytes());
    }

    
    public void readBytes(ChannelBuffer dst, int length) {
        // carefule
        if (length > dst.writableBytes()) {
            throw new IndexOutOfBoundsException();
        }
        readBytes(dst, dst.writerIndex(), length);
        dst.writerIndex(dst.writerIndex() + length);
    }

    
    public void readBytes(ChannelBuffer dst, int dstIndex, int length) {
        // careful
        if (readableBytes() < length) {
            throw new IndexOutOfBoundsException();
        }
        byte[] data = new byte[length];
        buffer.readBytes(data, 0, length);
        dst.setBytes(dstIndex, data, 0, length);
    }

    
    public ChannelBuffer readBytes(int length) {
        return new NettyBackedChannelBuffer(buffer.readBytes(length));
    }

    
    public void resetReaderIndex() {
        buffer.resetReaderIndex();
    }

    
    public void resetWriterIndex() {
        buffer.resetWriterIndex();
    }

    
    public int readerIndex() {
        return buffer.readerIndex();
    }

    
    public void readerIndex(int readerIndex) {
        buffer.readerIndex(readerIndex);
    }

    
    public void readBytes(OutputStream dst, int length) throws IOException {
        buffer.readBytes(dst, length);
    }

    
    public void setBytes(int index, byte[] src) {
        buffer.setBytes(index, src);
    }

    
    public void setBytes(int index, ChannelBuffer src) {
        // careful
        setBytes(index, src, src.readableBytes());
    }

    
    public void setBytes(int index, ChannelBuffer src, int length) {
        // careful
        if (length > src.readableBytes()) {
            throw new IndexOutOfBoundsException();
        }
        setBytes(index, src, src.readerIndex(), length);
        src.readerIndex(src.readerIndex() + length);
    }

    
    public void setIndex(int readerIndex, int writerIndex) {
        buffer.setIndex(readerIndex, writerIndex);
    }

    
    public void skipBytes(int length) {
        buffer.skipBytes(length);
    }

    
    public ByteBuffer toByteBuffer() {
        return buffer.nioBuffer();
    }

    
    public boolean writable() {
        return buffer.isWritable();
    }

    
    public int writableBytes() {
        return buffer.writableBytes();
    }

    
    public void writeByte(int value) {
        buffer.writeByte(value);
    }

    
    public void writeBytes(byte[] src) {
        buffer.writeBytes(src);
    }

    
    public void writeBytes(byte[] src, int index, int length) {
        buffer.writeBytes(src, index, length);
    }

    
    public void writeBytes(ByteBuffer src) {
        buffer.writeBytes(src);
    }

    
    public void writeBytes(ChannelBuffer src) {
        // careful
        writeBytes(src, src.readableBytes());
    }

    
    public void writeBytes(ChannelBuffer src, int length) {
        // careful
        if (length > src.readableBytes()) {
            throw new IndexOutOfBoundsException();
        }
        writeBytes(src, src.readerIndex(), length);
        src.readerIndex(src.readerIndex() + length);
    }

    
    public void writeBytes(ChannelBuffer src, int srcIndex, int length) {
        // careful
        byte[] data = new byte[length];
        src.getBytes(srcIndex, data, 0, length);
        writeBytes(data, 0, length);
    }

    
    public int writeBytes(InputStream src, int length) throws IOException {
        return buffer.writeBytes(src, length);
    }

    
    public int writerIndex() {
        return buffer.writerIndex();
    }

    
    public void writerIndex(int writerIndex) {
        buffer.writerIndex(writerIndex);
    }

    
    public int compareTo(ChannelBuffer o) {
        return ChannelBuffers.compare(this, o);
    }
}
