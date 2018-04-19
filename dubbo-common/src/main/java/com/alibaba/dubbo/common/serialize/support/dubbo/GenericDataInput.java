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
package com.alibaba.dubbo.common.serialize.support.dubbo;

import com.alibaba.dubbo.common.serialize.DataInput;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UTFDataFormatException;

/**
 * Default DataInput impl.
 * Not thread-safe.
 *
 * Dubbo 数据输入实现类
 */
public class GenericDataInput implements DataInput, GenericDataFlags {

    /**
     * 空字符串
     */
    private static final String EMPTY_STRING = "";

    /**
     * 空字节数组
     */
    private static final byte[] EMPTY_BYTES = {};

    /**
     * 输入流
     */
    private final InputStream mInput;
    /**
     * 读取 Buffer 数组
     */
    private final byte[] mBuffer;
    /**
     * {@link #mBuffer} 当前读取位置
     */
    private int mRead = 0;
    /**
     * {@link #mBuffer} 最大可读取位置
     */
    private int mPosition = 0;

    public GenericDataInput(InputStream is) {
        this(is, 1024);
    }

    public GenericDataInput(InputStream is, int buffSize) {
        mInput = is;
        mBuffer = new byte[buffSize];
    }

    @Override
    public boolean readBool() throws IOException {
        // 读取字节
        byte b = read0();
        // 判断 true / false
        switch (b) {
            case VARINT_0: // false
                return false;
            case VARINT_1: // true
                return true;
            default: // 非法
                throw new IOException("Tag error, expect BYTE_TRUE|BYTE_FALSE, but get " + b);
        }
    }

    @Override
    public byte readByte() throws IOException {
        // 读取字节
        byte b = read0();
        switch (b) {
            // 不符合 Varint 枚举值，读取字节返回
            case VARINT8:
                return read0();
            // 符合 Varint 枚举值，返回对应的值
            case VARINT_0:
                return 0;
            case VARINT_1:
                return 1;
            case VARINT_2:
                return 2;
            case VARINT_3:
                return 3;
            case VARINT_4:
                return 4;
            case VARINT_5:
                return 5;
            case VARINT_6:
                return 6;
            case VARINT_7:
                return 7;
            case VARINT_8:
                return 8;
            case VARINT_9:
                return 9;
            case VARINT_A:
                return 10;
            case VARINT_B:
                return 11;
            case VARINT_C:
                return 12;
            case VARINT_D:
                return 13;
            case VARINT_E:
                return 14;
            case VARINT_F:
                return 15;
            case VARINT_10:
                return 16;
            case VARINT_11:
                return 17;
            case VARINT_12:
                return 18;
            case VARINT_13:
                return 19;
            case VARINT_14:
                return 20;
            case VARINT_15:
                return 21;
            case VARINT_16:
                return 22;
            case VARINT_17:
                return 23;
            case VARINT_18:
                return 24;
            case VARINT_19:
                return 25;
            case VARINT_1A:
                return 26;
            case VARINT_1B:
                return 27;
            case VARINT_1C:
                return 28;
            case VARINT_1D:
                return 29;
            case VARINT_1E:
                return 30;
            case VARINT_1F:
                return 31;
            default: // 非法，抛出 IOException 异常
                throw new IOException("Tag error, expect VARINT, but get " + b);
        }
    }

    @Override
    public short readShort() throws IOException {
        return (short) readVarint32();
    }

    @Override
    public int readInt() throws IOException {
        return readVarint32();
    }

    @Override
    public long readLong() throws IOException {
        return readVarint64();
    }

    @Override
    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readVarint32());
    }

    @Override
    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readVarint64());
    }

    @Override
    public String readUTF() throws IOException {
        // 读取字节
        byte b = read0();
        switch (b) {
            // 字符串非空
            case OBJECT_BYTES:
                // 读取长度
                int len = readUInt();
                // 反序列化出字符串
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < len; i++) {
                    // 读取首位
                    byte b1 = read0();
                    if ((b1 & 0x80) == 0) { // [0, 128) ASCII 码
                        sb.append((char) b1);
                    } else if ((b1 & 0xE0) == 0xC0) { // [128, 2048)
                        byte b2 = read0();
                        sb.append((char) (((b1 & 0x1F) << 6) | (b2 & 0x3F)));
                    } else if ((b1 & 0xF0) == 0xE0) { // [2048, 65536)
                        byte b2 = read0(), b3 = read0();
                        sb.append((char) (((b1 & 0x0F) << 12) | ((b2 & 0x3F) << 6) | (b3 & 0x3F)));
                    } else
                        throw new UTFDataFormatException("Bad utf-8 encoding at " + b1);
                }
                return sb.toString();
            // NULL
            case OBJECT_NULL:
                return null;
            // 字符串为空
            case OBJECT_DUMMY:
                return EMPTY_STRING;
            default:
                throw new IOException("Tag error, expect BYTES|BYTES_NULL|BYTES_EMPTY, but get " + b);
        }
    }

    @Override
    public byte[] readBytes() throws IOException {
        // 读取字节
        byte b = read0();
        switch (b) {
            case OBJECT_BYTES: // 数组非空
                return read0(readUInt());
            case OBJECT_NULL: // NULL
                return null;
            case OBJECT_DUMMY: // 数组为空
                return EMPTY_BYTES;
            default:
                throw new IOException("Tag error, expect BYTES|BYTES_NULL|BYTES_EMPTY, but get " + b);
        }
    }

    /**
     * 读取 UInt
     *
     * @see GenericDataOutput#writeUInt(int)
     *
     * @return 正整数
     * @throws IOException 当 IO 发生异常时
     */
    public int readUInt() throws IOException {
        // 读取字节
        byte tmp = read0(); // 用于暂存当前读取结果
        // 【第一次】
        if (tmp < 0) { // 负数，意味着无后续
            return tmp & 0x7f;
        }
        int ret = tmp & 0x7f; // 最终结果
        // 【第二次】
        if ((tmp = read0()) < 0) { // 负数，意味着无后续
            ret |= (tmp & 0x7f) << 7; // 拼接 tmp + ret
        } else {
            ret |= tmp << 7;
            // 【第三次】
            if ((tmp = read0()) < 0) { // 负数，意味着无后续
                ret |= (tmp & 0x7f) << 14;
            } else {
                ret |= tmp << 14;
                // 【第四次】
                if ((tmp = read0()) < 0) { // 负数，意味着无后续
                    ret |= (tmp & 0x7f) << 21;
                // 【第五次】5 * 7 > 32 ，所以可以结束
                } else {
                    ret |= tmp << 21;
                    ret |= (read0() & 0x7f) << 28;
                }
            }
        }
        return ret;
    }

    /**
     * 读取字节
     *
     * @throws IOException 当发生 IO 异常时
     */
    protected byte read0() throws IOException {
        // 读取到达上限，从 mInput 读取到 mBuffer 中。
        if (mPosition == mRead) {
            fillBuffer();
        }
        // 从 mBuffer 中，读取字节。
        return mBuffer[mPosition++];
    }

    /**
     * 批量读取字节
     *
     * @param len 长度
     * @return 字节数组
     * @throws IOException 当发生 IO 异常时
     */
    protected byte[] read0(int len) throws IOException {
        int rem = mRead - mPosition;
        byte[] ret = new byte[len];
        // 未超过 mBuffer 剩余可读取，批量写入 mBuffer 中。mBuffer => ret
        if (len <= rem) {
            System.arraycopy(mBuffer, mPosition, ret, 0, len);
            mPosition += len;
        } else {
            // 部分批量写入 ref 中。mBuffer => ret
            System.arraycopy(mBuffer, mPosition, ret, 0, rem);
            mPosition = mRead;

            len -= rem;
            int read, pos = rem; // 新的 ret 读取起点

            // mInput => ret
            while (len > 0) {
                read = mInput.read(ret, pos, len);
                if (read == -1) {
                    throw new EOFException();
                }
                pos += read; // 新的 ret 读取起点
                len -= read;
            }
        }
        return ret;
    }

    private int readVarint32() throws IOException {
        // 读取首位 Byte 字节
        byte b = read0();
        //
        switch (b) {
            // 不符合 Varint 枚举值，读取 Tag + 具体值
            case VARINT8:
                return read0();
            case VARINT16: {
                byte b1 = read0(), b2 = read0();
                return (short) ((b1 & 0xff) |
                        ((b2 & 0xff) << 8));
            }
            case VARINT24: {
                byte b1 = read0(), b2 = read0(), b3 = read0();
                int ret = (b1 & 0xff) |
                        ((b2 & 0xff) << 8) |
                        ((b3 & 0xff) << 16);
                if (b3 < 0) { // 补齐负数的高位
                    return ret | 0xff000000;
                }
                return ret;
            }
            case VARINT32: {
                byte b1 = read0(), b2 = read0(), b3 = read0(), b4 = read0();
                return ((b1 & 0xff) |
                        ((b2 & 0xff) << 8) |
                        ((b3 & 0xff) << 16) |
                        ((b4 & 0xff) << 24));
            }
            // 符合 Varint 枚举值，返回对应的值
            case VARINT_NF:
                return -15;
            case VARINT_NE:
                return -14;
            case VARINT_ND:
                return -13;
            case VARINT_NC:
                return -12;
            case VARINT_NB:
                return -11;
            case VARINT_NA:
                return -10;
            case VARINT_N9:
                return -9;
            case VARINT_N8:
                return -8;
            case VARINT_N7:
                return -7;
            case VARINT_N6:
                return -6;
            case VARINT_N5:
                return -5;
            case VARINT_N4:
                return -4;
            case VARINT_N3:
                return -3;
            case VARINT_N2:
                return -2;
            case VARINT_N1:
                return -1;
            case VARINT_0:
                return 0;
            case VARINT_1:
                return 1;
            case VARINT_2:
                return 2;
            case VARINT_3:
                return 3;
            case VARINT_4:
                return 4;
            case VARINT_5:
                return 5;
            case VARINT_6:
                return 6;
            case VARINT_7:
                return 7;
            case VARINT_8:
                return 8;
            case VARINT_9:
                return 9;
            case VARINT_A:
                return 10;
            case VARINT_B:
                return 11;
            case VARINT_C:
                return 12;
            case VARINT_D:
                return 13;
            case VARINT_E:
                return 14;
            case VARINT_F:
                return 15;
            case VARINT_10:
                return 16;
            case VARINT_11:
                return 17;
            case VARINT_12:
                return 18;
            case VARINT_13:
                return 19;
            case VARINT_14:
                return 20;
            case VARINT_15:
                return 21;
            case VARINT_16:
                return 22;
            case VARINT_17:
                return 23;
            case VARINT_18:
                return 24;
            case VARINT_19:
                return 25;
            case VARINT_1A:
                return 26;
            case VARINT_1B:
                return 27;
            case VARINT_1C:
                return 28;
            case VARINT_1D:
                return 29;
            case VARINT_1E:
                return 30;
            case VARINT_1F:
                return 31;
            default:
                throw new IOException("Tag error, expect VARINT, but get " + b);
        }
    }

    private long readVarint64() throws IOException {
        byte b = read0();

        switch (b) {
            case VARINT8:
                return read0();
            case VARINT16: {
                byte b1 = read0(), b2 = read0();
                return (short) ((b1 & 0xff) | ((b2 & 0xff) << 8));
            }
            case VARINT24: {
                byte b1 = read0(), b2 = read0(), b3 = read0();
                int ret = (b1 & 0xff) | ((b2 & 0xff) << 8) | ((b3 & 0xff) << 16);
                if (b3 < 0) { // 补齐负数的高位
                    return ret | 0xff000000;
                }
                return ret;
            }
            case VARINT32: {
                byte b1 = read0(), b2 = read0(), b3 = read0(), b4 = read0();
                return ((b1 & 0xff) |
                        ((b2 & 0xff) << 8) |
                        ((b3 & 0xff) << 16) |
                        ((b4 & 0xff) << 24));
            }
            case VARINT40: {
                byte b1 = read0(), b2 = read0(), b3 = read0(), b4 = read0(), b5 = read0();
                long ret = ((long) b1 & 0xff) |
                        (((long) b2 & 0xff) << 8) |
                        (((long) b3 & 0xff) << 16) |
                        (((long) b4 & 0xff) << 24) |
                        (((long) b5 & 0xff) << 32);
                if (b5 < 0) { // 补齐负数的高位
                    return ret | 0xffffff0000000000L;
                }
                return ret;
            }
            case VARINT48: {
                byte b1 = read0(), b2 = read0(), b3 = read0(), b4 = read0(), b5 = read0(), b6 = read0();
                long ret = ((long) b1 & 0xff) |
                        (((long) b2 & 0xff) << 8) |
                        (((long) b3 & 0xff) << 16) |
                        (((long) b4 & 0xff) << 24) |
                        (((long) b5 & 0xff) << 32) |
                        (((long) b6 & 0xff) << 40);
                if (b6 < 0) { // 补齐负数的高位
                    return ret | 0xffff000000000000L;
                }
                return ret;
            }
            case VARINT56: {
                byte b1 = read0(), b2 = read0(), b3 = read0(), b4 = read0(), b5 = read0(), b6 = read0(), b7 = read0();
                long ret = ((long) b1 & 0xff) |
                        (((long) b2 & 0xff) << 8) |
                        (((long) b3 & 0xff) << 16) |
                        (((long) b4 & 0xff) << 24) |
                        (((long) b5 & 0xff) << 32) |
                        (((long) b6 & 0xff) << 40) |
                        (((long) b7 & 0xff) << 48);
                if (b7 < 0) {
                    return ret | 0xff00000000000000L;
                }
                return ret;
            }
            case VARINT64: {
                byte b1 = read0(), b2 = read0(), b3 = read0(), b4 = read0();
                byte b5 = read0(), b6 = read0(), b7 = read0(), b8 = read0();
                return (((long) b1 & 0xff) |
                        (((long) b2 & 0xff) << 8) |
                        (((long) b3 & 0xff) << 16) |
                        (((long) b4 & 0xff) << 24) |
                        (((long) b5 & 0xff) << 32) |
                        (((long) b6 & 0xff) << 40) |
                        (((long) b7 & 0xff) << 48) |
                        (((long) b8 & 0xff) << 56));
            }
            case VARINT_NF:
                return -15;
            case VARINT_NE:
                return -14;
            case VARINT_ND:
                return -13;
            case VARINT_NC:
                return -12;
            case VARINT_NB:
                return -11;
            case VARINT_NA:
                return -10;
            case VARINT_N9:
                return -9;
            case VARINT_N8:
                return -8;
            case VARINT_N7:
                return -7;
            case VARINT_N6:
                return -6;
            case VARINT_N5:
                return -5;
            case VARINT_N4:
                return -4;
            case VARINT_N3:
                return -3;
            case VARINT_N2:
                return -2;
            case VARINT_N1:
                return -1;
            case VARINT_0:
                return 0;
            case VARINT_1:
                return 1;
            case VARINT_2:
                return 2;
            case VARINT_3:
                return 3;
            case VARINT_4:
                return 4;
            case VARINT_5:
                return 5;
            case VARINT_6:
                return 6;
            case VARINT_7:
                return 7;
            case VARINT_8:
                return 8;
            case VARINT_9:
                return 9;
            case VARINT_A:
                return 10;
            case VARINT_B:
                return 11;
            case VARINT_C:
                return 12;
            case VARINT_D:
                return 13;
            case VARINT_E:
                return 14;
            case VARINT_F:
                return 15;
            case VARINT_10:
                return 16;
            case VARINT_11:
                return 17;
            case VARINT_12:
                return 18;
            case VARINT_13:
                return 19;
            case VARINT_14:
                return 20;
            case VARINT_15:
                return 21;
            case VARINT_16:
                return 22;
            case VARINT_17:
                return 23;
            case VARINT_18:
                return 24;
            case VARINT_19:
                return 25;
            case VARINT_1A:
                return 26;
            case VARINT_1B:
                return 27;
            case VARINT_1C:
                return 28;
            case VARINT_1D:
                return 29;
            case VARINT_1E:
                return 30;
            case VARINT_1F:
                return 31;
            default:
                throw new IOException("Tag error, expect VARINT, but get " + b);
        }
    }

    private void fillBuffer() throws IOException {
        // 重置 mPosition
        mPosition = 0;
        // 读取 mInput 到 mBuffer 中
        mRead = mInput.read(mBuffer);
        // 未读取到，抛出 EOFException 异常
        if (mRead == -1) {
            mRead = 0;
            throw new EOFException();
        }
    }

}