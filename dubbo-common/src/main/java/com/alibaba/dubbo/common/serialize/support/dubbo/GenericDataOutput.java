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

import com.alibaba.dubbo.common.serialize.DataOutput;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Default data output impl.
 * Not thread-safe.
 *
 * Dubbo 数据输出实现类
 */
@SuppressWarnings("Duplicates")
public class GenericDataOutput implements DataOutput, GenericDataFlags {

    /**
     * 默认 {@link #mCharBuf} 大小
     */
    private static final int CHAR_BUF_SIZE = 256;
    /**
     * 序列化字符串的临时结果的 Buffer 数组，用于 {@link #writeUTF(String)} 中。
     */
    private final char[] mCharBuf = new char[CHAR_BUF_SIZE];

    /**
     * 序列化 Varint 的临时结果的 Buffer 数组，用于 {@link #writeVarint32(int)} 和 {@link #writeVarint64(long)} 中。
     */
    private final byte[] mTemp = new byte[9];

    /**
     * 序列化结果的 Buffer 数组
     */
    private final byte[] mBuffer;
    /**
     * {@link #mBuffer} 容量大小
     */
    private final int mLimit;
    /**
     * {@link #mBuffer} 当前写入位置
     */
    private int mPosition = 0;
    /**
     * 结果输出
     */
    private final OutputStream mOutput;

    public GenericDataOutput(OutputStream out) {
        this(out, 1024);
    }

    public GenericDataOutput(OutputStream out, int buffSize) {
        mOutput = out;
        mLimit = buffSize;
        mBuffer = new byte[buffSize];
    }

    @Override
    public void writeBool(boolean v) throws IOException {
        write0(v ? VARINT_1 : VARINT_0);
    }

    @Override
    public void writeByte(byte v) throws IOException {
        switch (v) {
            // TODO 【8034】为什么没有负数的枚举
            // 符合 Varint 枚举值，写入对应的枚举值
            case 0:
                write0(VARINT_0);
                break;
            case 1:
                write0(VARINT_1);
                break;
            case 2:
                write0(VARINT_2);
                break;
            case 3:
                write0(VARINT_3);
                break;
            case 4:
                write0(VARINT_4);
                break;
            case 5:
                write0(VARINT_5);
                break;
            case 6:
                write0(VARINT_6);
                break;
            case 7:
                write0(VARINT_7);
                break;
            case 8:
                write0(VARINT_8);
                break;
            case 9:
                write0(VARINT_9);
                break;
            case 10:
                write0(VARINT_A);
                break;
            case 11:
                write0(VARINT_B);
                break;
            case 12:
                write0(VARINT_C);
                break;
            case 13:
                write0(VARINT_D);
                break;
            case 14:
                write0(VARINT_E);
                break;
            case 15:
                write0(VARINT_F);
                break;
            case 16:
                write0(VARINT_10);
                break;
            case 17:
                write0(VARINT_11);
                break;
            case 18:
                write0(VARINT_12);
                break;
            case 19:
                write0(VARINT_13);
                break;
            case 20:
                write0(VARINT_14);
                break;
            case 21:
                write0(VARINT_15);
                break;
            case 22:
                write0(VARINT_16);
                break;
            case 23:
                write0(VARINT_17);
                break;
            case 24:
                write0(VARINT_18);
                break;
            case 25:
                write0(VARINT_19);
                break;
            case 26:
                write0(VARINT_1A);
                break;
            case 27:
                write0(VARINT_1B);
                break;
            case 28:
                write0(VARINT_1C);
                break;
            case 29:
                write0(VARINT_1D);
                break;
            case 30:
                write0(VARINT_1E);
                break;
            case 31:
                write0(VARINT_1F);
                break;
            // 不符合 Varint 枚举值，写入 Tag + 具体值
            default:
                // 写入 VARINT8
                write0(VARINT8);
                // 写入 BYTE 具体值
                write0(v);
        }
    }

    @Override
    public void writeShort(short v) throws IOException {
        writeVarint32(v);
    }

    @Override
    public void writeInt(int v) throws IOException {
        writeVarint32(v);
    }

    @Override
    public void writeLong(long v) throws IOException {
        writeVarint64(v);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        writeVarint32(Float.floatToRawIntBits(v));
    }

    @Override
    public void writeDouble(double v) throws IOException {
        writeVarint64(Double.doubleToRawLongBits(v));
    }

    @Override
    public void writeUTF(String v) throws IOException {
        // NULL ，使用 OBJECT_NULL 写入 mBuffer
        if (v == null) {
            write0(OBJECT_NULL);
        } else {
            // 空字符串，使用 OBJECT_DUMMY 写入 mBuffer
            int len = v.length();
            if (len == 0) {
                write0(OBJECT_DUMMY);
            // 字符串非空，写入 OBJECT_BYTES + Length + 具体数据到 mBuffer
            } else {
                // 写入 OBJECT_BYTES 到 mBuffer 中
                write0(OBJECT_BYTES);
                // 写入 Length 到 mBuffer 中
                writeUInt(len);

                int off = 0,
                    limit = mLimit - 3, // -3 的原因，因为若 Char 在 [2048, 65536) 范围内，需要占用三个字节，事先无法得知。
                    size;
                char[] buf = mCharBuf;
                do {
                    // 读取数量，不超过 CHAR_BUF_SIZE 上限，同时不超过可读上限
                    size = Math.min(len - off, CHAR_BUF_SIZE);
                    // 读取字符串到 buf 中
                    v.getChars(off, off + size, buf, 0);

                    // 写入数据到 mBuffer 中
                    for (int i = 0; i < size; i++) {
                        char c = buf[i];
                        // Java Character 数据范围为 [0, 65535]
                        if (mPosition > limit) {
                            if (c < 0x80) { // [0, 128) ASCII 码
                                // 0X80 => 10 00 00 00 取七位 [0, 64)

                                write0((byte) c);
                            } else if (c < 0x800) { // [128, 2048)
                                // 0xC0 => 11 00 00 00 取六位 [0, 32)
                                // 0x80 => 10 00 00 00 取七位 [0, 64)

                                // 0x1F => 00 01 11 11
                                // 0x3F => 00 11 11 11

                                write0((byte) (0xC0 | ((c >> 6) & 0x1F)));
                                write0((byte) (0x80 | (c & 0x3F)));
                            } else { // [2048, 65536)
                                // 0xE0 => 11 10 00 00 取五位 [0, 15]
                                // 0x80 => 10 00 00 00 取七位 [0, 63]
                                // 0x80 => 10 00 00 00 取七位 [0, 63]

                                // 0x0F => 00 00 11 11
                                // 0x3F => 00 11 11 11
                                // 0x3F => 00 11 11 11

                                write0((byte) (0xE0 | ((c >> 12) & 0x0F)));
                                write0((byte) (0x80 | ((c >> 6) & 0x3F)));
                                write0((byte) (0x80 | (c & 0x3F)));
                            }
                        } else {
                            if (c < 0x80) {
                                mBuffer[mPosition++] = (byte) c;
                            } else if (c < 0x800) {
                                mBuffer[mPosition++] = (byte) (0xC0 | ((c >> 6) & 0x1F));
                                mBuffer[mPosition++] = (byte) (0x80 | (c & 0x3F));
                            } else {
                                mBuffer[mPosition++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
                                mBuffer[mPosition++] = (byte) (0x80 | ((c >> 6) & 0x3F));
                                mBuffer[mPosition++] = (byte) (0x80 | (c & 0x3F));
                            }
                        }
                    }

                    // 计算 buf 新的开始读取位置。
                    off += size;
                } while (off < len);
            }
        }
    }

    @Override
    public void writeBytes(byte[] b) throws IOException {
        // NULL ，使用 OBJECT_NULL 写入 mBuffer
        if (b == null) {
            write0(OBJECT_NULL);
        // 其他，写入 mBuffer
        } else {
            writeBytes(b, 0, b.length);
        }
    }

    @Override
    public void writeBytes(byte[] b, int off, int len) throws IOException {
        // 空数组，使用 OBJECT_DUMMY 写入 mBuffer
        if (len == 0) {
            write0(OBJECT_DUMMY);
        // 数组非空，写入 OBJECT_BYTES + Length + 具体数据到 mBuffer
        } else {
            write0(OBJECT_BYTES);
            writeUInt(len); // UInt
            write0(b, off, len);
        }
    }

    @Override
    public void flushBuffer() throws IOException {
        if (mPosition > 0) {
            // 写入 mOutput
            mOutput.write(mBuffer, 0, mPosition);
            // 重置当前写入位置
            mPosition = 0;
        }
    }

    /**
     * 写入 UInt
     *
     * 变长正整数。原理是：
     * 因为是正整数，所以可以使用 Byte 最高位的 1 ，原来用来表示负数，现在来表示是否有后续的 BYTE ，也是正整数的一部分。
     *
     * @param v 正整数
     * @throws IOException 当 IO 发生异常时
     */
    public void writeUInt(int v) throws IOException {
        byte tmp;
        // 循环写入
        while (true) {
            // 获得最后 7 Bits
            tmp = (byte) (v & 0x7f);
            // 无后续的 Byte ，修改 tmp 首 Bit 为 1 ，写入 mBuffer 中，并结束。
            if ((v >>>= 7) == 0) {
                write0((byte) (tmp | 0x80));
                return;
            // 有后续的 Byte ，写入 mBuffer 中
            } else {
                write0(tmp);
            }
        }
    }

    /**
     * 写入 mBuffer 中
     *
     * @param b 字节
     * @throws IOException 当发生 IO 异常时
     */
    protected void write0(byte b) throws IOException {
        // 超过 mBuffer 容量上限，刷入 mOutput 中
        if (mPosition == mLimit) {
            flushBuffer();
        }
        // 写入 mBuffer 中。
        mBuffer[mPosition++] = b;
    }

    /**
     * 批量写入 mBuffer 中
     *
     * @param b 字节数组
     * @param off 开始位置
     * @param len 长度
     * @throws IOException 当发生 IO 异常时
     */
    protected void write0(byte[] b, int off, int len) throws IOException {
        int rem = mLimit - mPosition;
        // 未超过 mBuffer 容量上限，批量写入 mBuffer 中
        if (rem > len) {
            System.arraycopy(b, off, mBuffer, mPosition, len);
            mPosition += len;
        } else {
            // 部分批量写满 mBuffer 中
            System.arraycopy(b, off, mBuffer, mPosition, rem);
            mPosition = mLimit;
            // 刷入 mOutput 中
            flushBuffer();

            off += rem; // 新的开始位置
            len -= rem; // 新的长度

            // 未超过 mBuffer 容量上限，批量写入 mBuffer 中
            if (mLimit > len) {
                System.arraycopy(b, off, mBuffer, 0, len);
                mPosition = len;
            // 超过 mBuffer 容量上限，批量写入 mOutput 中
            } else {
                mOutput.write(b, off, len);
            }
        }
    }

    private void writeVarint32(int v) throws IOException {
        switch (v) {
            // 符合 Varint 枚举值，写入对应的枚举值
            case -15:
                write0(VARINT_NF);
                break;
            case -14:
                write0(VARINT_NE);
                break;
            case -13:
                write0(VARINT_ND);
                break;
            case -12:
                write0(VARINT_NC);
                break;
            case -11:
                write0(VARINT_NB);
                break;
            case -10:
                write0(VARINT_NA);
                break;
            case -9:
                write0(VARINT_N9);
                break;
            case -8:
                write0(VARINT_N8);
                break;
            case -7:
                write0(VARINT_N7);
                break;
            case -6:
                write0(VARINT_N6);
                break;
            case -5:
                write0(VARINT_N5);
                break;
            case -4:
                write0(VARINT_N4);
                break;
            case -3:
                write0(VARINT_N3);
                break;
            case -2:
                write0(VARINT_N2);
                break;
            case -1:
                write0(VARINT_N1);
                break;
            case 0:
                write0(VARINT_0);
                break;
            case 1:
                write0(VARINT_1);
                break;
            case 2:
                write0(VARINT_2);
                break;
            case 3:
                write0(VARINT_3);
                break;
            case 4:
                write0(VARINT_4);
                break;
            case 5:
                write0(VARINT_5);
                break;
            case 6:
                write0(VARINT_6);
                break;
            case 7:
                write0(VARINT_7);
                break;
            case 8:
                write0(VARINT_8);
                break;
            case 9:
                write0(VARINT_9);
                break;
            case 10:
                write0(VARINT_A);
                break;
            case 11:
                write0(VARINT_B);
                break;
            case 12:
                write0(VARINT_C);
                break;
            case 13:
                write0(VARINT_D);
                break;
            case 14:
                write0(VARINT_E);
                break;
            case 15:
                write0(VARINT_F);
                break;
            case 16:
                write0(VARINT_10);
                break;
            case 17:
                write0(VARINT_11);
                break;
            case 18:
                write0(VARINT_12);
                break;
            case 19:
                write0(VARINT_13);
                break;
            case 20:
                write0(VARINT_14);
                break;
            case 21:
                write0(VARINT_15);
                break;
            case 22:
                write0(VARINT_16);
                break;
            case 23:
                write0(VARINT_17);
                break;
            case 24:
                write0(VARINT_18);
                break;
            case 25:
                write0(VARINT_19);
                break;
            case 26:
                write0(VARINT_1A);
                break;
            case 27:
                write0(VARINT_1B);
                break;
            case 28:
                write0(VARINT_1C);
                break;
            case 29:
                write0(VARINT_1D);
                break;
            case 30:
                write0(VARINT_1E);
                break;
            case 31:
                write0(VARINT_1F);
                break;
            // 不符合 Varint 枚举值，写入 Tag + 具体值
            default:
                int t = v, // 值
                    ix = 0; // 当前写入位置
                byte[] b = mTemp; // 字节数组
                // 顺序读取字节，存到 mTemp 中
                while (true) {
                    b[++ix] = (byte) (v & 0xff); // 大于等于 128 时，会截取到最高位的 1 ，变成负数。
                    if ((v >>>= 8) == 0) { // 无可读字节
                        break;
                    }
                }

                if (t > 0) { // 正数
                    // [ 0a e2 => 0a e2 00 ] [ 92 => 92 00 ]
                    // 最后一次取余，大于等于 128 时，在 (byte) 转换后，变成了负数，需要补一个 0 的 BYTE 到 mTemp 中，否则反序列化后会被误认为负数。
                    if (b[ix] < 0) {
                        b[++ix] = 0;
                    }
                } else { // 负数
                    // [ 01 ff ff ff => 01 ff ] [ e0 ff ff ff => e0 ]
                    // 负数使用补码表示，高位是大量的 1 ，需要去除。
                    // 另外，LONG 的位数比 INT 更多，所以，相同数字，LONG 型会比 INT 型更多，例如 long v = -662L 和 int v = -662 。
                    while (b[ix] == (byte) 0xff && b[ix - 1] < 0) {
                        ix--;
                    }
                }

                // 写入 Tag ，到首 Byte 位
                b[0] = (byte) (VARINT + ix - 1);
                // 写入 Tag + Bytes 到 mBuffer 中
                write0(b, 0, ix + 1);
        }
    }

    private void writeVarint64(long v) throws IOException {
        // 数据范围在 INT 内
        int i = (int) v;
        if (v == i) {
            writeVarint32(i);
        // 数据范围在 LONG 内，不符合 Varint 枚举值，写入 Tag + 具体值。和 writeVarint32 是一致的
        } else {
            long t = v;
            int ix = 0;
            byte[] b = mTemp;

            while (true) {
                b[++ix] = (byte) (v & 0xff);
                if ((v >>>= 8) == 0)
                    break;
            }

            if (t > 0) {
                // [ 0a e2 => 0a e2 00 ] [ 92 => 92 00 ]
                if (b[ix] < 0)
                    b[++ix] = 0;
            } else {
                // [ 01 ff ff ff => 01 ff ] [ e0 ff ff ff => e0 ]
                while (b[ix] == (byte) 0xff && b[ix - 1] < 0)
                    ix--;
            }

            b[0] = (byte) (VARINT + ix - 1);
            write0(b, 0, ix + 1);
        }
    }

}