/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.common.io;

import junit.framework.TestCase;
import org.junit.Assert;

public class BytesTest extends TestCase {
    private static final byte[] b1 = "adpfioha;eoh;aldfadl;kfadslkfdajfio123431241235123davas;odvwe;lmzcoqpwoewqogineopwqihwqetup\n\tejqf;lajsfd中文字符0da0gsaofdsf==adfasdfs".getBytes();
    static byte[] bytes1 = {3, 12, 14, 41, 12, 2, 3, 12, 4, 67, 23};
    static byte[] bytes2 = {3, 12, 14, 41, 12, 2, 3, 12, 4, 67};

    private static void assertSame(byte[] b1, byte[] b2) {
        assertEquals(b1.length, b2.length);
        for (int i = 0; i < b1.length; i++)
            assertEquals(b1[i], b2[i]);
    }

    // tb-remoting codec method.
    static public byte[] int2bytes(int x) {
        byte[] bb = new byte[4];
        bb[0] = (byte) (x >> 24);
        bb[1] = (byte) (x >> 16);
        bb[2] = (byte) (x >> 8);
        bb[3] = (byte) (x >> 0);
        return bb;
    }

    static public int bytes2int(byte[] bb, int idx) {
        return ((bb[idx + 0] & 0xFF) << 24)
                | ((bb[idx + 1] & 0xFF) << 16)
                | ((bb[idx + 2] & 0xFF) << 8)
                | ((bb[idx + 3] & 0xFF) << 0);
    }

    static public byte[] long2bytes(long x) {
        byte[] bb = new byte[8];
        bb[0] = (byte) (x >> 56);
        bb[1] = (byte) (x >> 48);
        bb[2] = (byte) (x >> 40);
        bb[3] = (byte) (x >> 32);
        bb[4] = (byte) (x >> 24);
        bb[5] = (byte) (x >> 16);
        bb[6] = (byte) (x >> 8);
        bb[7] = (byte) (x >> 0);
        return bb;
    }

    static public long bytes2long(byte[] bb, int idx) {
        return (((long) bb[idx + 0] & 0xFF) << 56)
                | (((long) bb[idx + 1] & 0xFF) << 48)
                | (((long) bb[idx + 2] & 0xFF) << 40)
                | (((long) bb[idx + 3] & 0xFF) << 32)
                | (((long) bb[idx + 4] & 0xFF) << 24)
                | (((long) bb[idx + 5] & 0xFF) << 16)
                | (((long) bb[idx + 6] & 0xFF) << 8)
                | (((long) bb[idx + 7] & 0xFF) << 0);
    }

    public void testMain() throws Exception {
        short s = (short) 0xabcd;
        assertEquals(s, Bytes.bytes2short(Bytes.short2bytes(s)));

        int i = 198284;
        assertEquals(i, Bytes.bytes2int(Bytes.int2bytes(i)));

        long l = 13841747174l;
        assertEquals(l, Bytes.bytes2long(Bytes.long2bytes(l)));

        float f = 1.3f;
        assertEquals(f, Bytes.bytes2float(Bytes.float2bytes(f)));

        double d = 11213.3;
        assertEquals(d, Bytes.bytes2double(Bytes.double2bytes(d)));

        assertSame(Bytes.int2bytes(i), int2bytes(i));
        assertSame(Bytes.long2bytes(l), long2bytes(l));
    }

    public void testBase64() throws Exception {
        String str = Bytes.bytes2base64(b1);
        byte[] b2 = Bytes.base642bytes(str);
        assertSame(b1, b2);
    }

    // 把失败的情况，失败Case加的防护网：
    // 当有填充字符时，会失败！
    public void testBase64_s2b2s_FailCaseLog() throws Exception {
        String s1 = Bytes.bytes2base64(bytes1);
        byte[] out1 = Bytes.base642bytes(s1);
        Assert.assertArrayEquals(bytes1, out1);


        String s2 = Bytes.bytes2base64(bytes2);
        byte[] out2 = Bytes.base642bytes(s2);
        Assert.assertArrayEquals(bytes2, out2);
    }

    public void testHex() throws Exception {
        String str = Bytes.bytes2hex(b1);
        assertSame(b1, Bytes.hex2bytes(str));
    }
}