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
package org.apache.dubbo.remoting.http.ssl;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;

public class BASE64Decoder {
    private static final char[] pem_array = new char[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'};
    private static final byte[] pem_convert_array = new byte[256];
    byte[] decode_buffer = new byte[4];

    public BASE64Decoder() {
    }

    protected int bytesPerAtom() {
        return 4;
    }

    protected int bytesPerLine() {
        return 72;
    }

    protected void decodeAtom(PushbackInputStream var1, OutputStream var2, int var3) throws IOException {
        byte var5 = -1;
        byte var6 = -1;
        byte var7 = -1;
        byte var8 = -1;
        if (var3 < 2) {
            throw new CEFormatException("BASE64Decoder: Not enough bytes for an atom.");
        } else {
            int var4;
            do {
                var4 = var1.read();
                if (var4 == -1) {
                    throw new CEStreamExhausted();
                }
            } while (var4 == 10 || var4 == 13);

            this.decode_buffer[0] = (byte) var4;
            var4 = this.readFully(var1, this.decode_buffer, 1, var3 - 1);
            if (var4 == -1) {
                throw new CEStreamExhausted();
            } else {
                if (var3 > 3 && this.decode_buffer[3] == 61) {
                    var3 = 3;
                }

                if (var3 > 2 && this.decode_buffer[2] == 61) {
                    var3 = 2;
                }

                switch (var3) {
                    case 4:
                        var8 = pem_convert_array[this.decode_buffer[3] & 255];
                    case 3:
                        var7 = pem_convert_array[this.decode_buffer[2] & 255];
                    case 2:
                        var6 = pem_convert_array[this.decode_buffer[1] & 255];
                        var5 = pem_convert_array[this.decode_buffer[0] & 255];
                    default:
                        switch (var3) {
                            case 2:
                                var2.write((byte) (var5 << 2 & 252 | var6 >>> 4 & 3));
                                break;
                            case 3:
                                var2.write((byte) (var5 << 2 & 252 | var6 >>> 4 & 3));
                                var2.write((byte) (var6 << 4 & 240 | var7 >>> 2 & 15));
                                break;
                            case 4:
                                var2.write((byte) (var5 << 2 & 252 | var6 >>> 4 & 3));
                                var2.write((byte) (var6 << 4 & 240 | var7 >>> 2 & 15));
                                var2.write((byte) (var7 << 6 & 192 | var8 & 63));
                        }

                }
            }
        }
    }

    static {
        int var0;
        for (var0 = 0; var0 < 255; ++var0) {
            pem_convert_array[var0] = -1;
        }

        for (var0 = 0; var0 < pem_array.length; ++var0) {
            pem_convert_array[pem_array[var0]] = (byte) var0;
        }

    }

    protected int readFully(InputStream var1, byte[] var2, int var3, int var4) throws IOException {
        for (int var5 = 0; var5 < var4; ++var5) {
            int var6 = var1.read();
            if (var6 == -1) {
                return var5 == 0 ? -1 : var5;
            }

            var2[var5 + var3] = (byte) var6;
        }

        return var4;
    }

    public void decodeBuffer(InputStream var1, OutputStream var2) throws IOException {
        int var4 = 0;
        PushbackInputStream var5 = new PushbackInputStream(var1);
        this.decodeBufferPrefix(var5, var2);

        while (true) {
            try {
                int var6 = this.decodeLinePrefix(var5, var2);

                int var3;
                for (var3 = 0; var3 + this.bytesPerAtom() < var6; var3 += this.bytesPerAtom()) {
                    this.decodeAtom(var5, var2, this.bytesPerAtom());
                    var4 += this.bytesPerAtom();
                }

                if (var3 + this.bytesPerAtom() == var6) {
                    this.decodeAtom(var5, var2, this.bytesPerAtom());
                    var4 += this.bytesPerAtom();
                } else {
                    this.decodeAtom(var5, var2, var6 - var3);
                    var4 += var6 - var3;
                }

                this.decodeLineSuffix(var5, var2);
            } catch (CEStreamExhausted var8) {
                this.decodeBufferSuffix(var5, var2);
                return;
            }
        }
    }

    public byte[] decodeBuffer(String var1) throws IOException {
        byte[] var2 = new byte[var1.length()];
        var1.getBytes(0, var1.length(), var2, 0);
        ByteArrayInputStream var3 = new ByteArrayInputStream(var2);
        ByteArrayOutputStream var4 = new ByteArrayOutputStream();
        this.decodeBuffer(var3, var4);
        return var4.toByteArray();
    }


    protected void decodeBufferPrefix(PushbackInputStream var1, OutputStream var2) throws IOException {
    }

    protected void decodeBufferSuffix(PushbackInputStream var1, OutputStream var2) throws IOException {
    }

    protected int decodeLinePrefix(PushbackInputStream var1, OutputStream var2) throws IOException {
        return this.bytesPerLine();
    }

    protected void decodeLineSuffix(PushbackInputStream var1, OutputStream var2) throws IOException {
    }

    private static class CEFormatException extends IOException {
        static final long serialVersionUID = -7139121221067081482L;

        public CEFormatException(String var1) {
            super(var1);
        }
    }

    private class CEStreamExhausted extends IOException {
        static final long serialVersionUID = -5889118049525891904L;

        public CEStreamExhausted() {
        }
    }

}
