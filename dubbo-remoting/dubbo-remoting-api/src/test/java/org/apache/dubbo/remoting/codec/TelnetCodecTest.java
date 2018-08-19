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
package org.apache.dubbo.remoting.codec;


import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.Codec2;
import org.apache.dubbo.remoting.buffer.ChannelBuffer;
import org.apache.dubbo.remoting.buffer.ChannelBuffers;
import org.apache.dubbo.remoting.telnet.codec.TelnetCodec;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;

public class TelnetCodecTest {
    protected Codec2 codec;
    byte[] UP = new byte[]{27, 91, 65};
    byte[] DOWN = new byte[]{27, 91, 66};
    //======================================================
    URL url = URL.valueOf("dubbo://10.20.30.40:20880");

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        codec = new TelnetCodec();
    }

    protected AbstractMockChannel getServerSideChannel(URL url) {
        url = url.addParameter(AbstractMockChannel.LOCAL_ADDRESS, url.getAddress())
                .addParameter(AbstractMockChannel.REMOTE_ADDRESS, "127.0.0.1:12345");
        AbstractMockChannel channel = new AbstractMockChannel(url);
        return channel;
    }

    protected AbstractMockChannel getCliendSideChannel(URL url) {
        url = url.addParameter(AbstractMockChannel.LOCAL_ADDRESS, "127.0.0.1:12345")
                .addParameter(AbstractMockChannel.REMOTE_ADDRESS, url.getAddress());
        AbstractMockChannel channel = new AbstractMockChannel(url);
        return channel;
    }

    protected byte[] join(byte[] in1, byte[] in2) {
        byte[] ret = new byte[in1.length + in2.length];
        System.arraycopy(in1, 0, ret, 0, in1.length);
        System.arraycopy(in2, 0, ret, in1.length, in2.length);
        return ret;
    }

    protected byte[] objectToByte(Object obj) {
        byte[] bytes;
        if (obj instanceof String) {
            bytes = ((String) obj).getBytes();
        } else if (obj instanceof byte[]) {
            bytes = (byte[]) obj;
        } else {
            try {
                //object to bytearray
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                ObjectOutputStream oo = new ObjectOutputStream(bo);
                oo.writeObject(obj);
                bytes = bo.toByteArray();
                bo.close();
                oo.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return (bytes);
    }

    protected Object byteToObject(byte[] objBytes) throws Exception {
        if (objBytes == null || objBytes.length == 0) {
            return null;
        }
        ByteArrayInputStream bi = new ByteArrayInputStream(objBytes);
        ObjectInputStream oi = new ObjectInputStream(bi);
        return oi.readObject();
    }

    protected void testDecode_assertEquals(byte[] request, Object ret) throws IOException {
        testDecode_assertEquals(request, ret, true);
    }

    protected void testDecode_assertEquals(byte[] request, Object ret, boolean isServerside) throws IOException {
        //init channel
        Channel channel = isServerside ? getServerSideChannel(url) : getCliendSideChannel(url);
        //init request string
        ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(request);

        //decode
        Object obj = codec.decode(channel, buffer);
        Assert.assertEquals(ret, obj);
    }


    protected void testEecode_assertEquals(Object request, byte[] ret, boolean isServerside) throws IOException {
        //init channel
        Channel channel = isServerside ? getServerSideChannel(url) : getCliendSideChannel(url);

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer(1024);

        codec.encode(channel, buffer, request);
        byte[] data = new byte[buffer.readableBytes()];
        buffer.readBytes(data);

        Assert.assertEquals(ret.length, data.length);
        for (int i = 0; i < ret.length; i++) {
            if (ret[i] != data[i]) {
                Assert.fail();
            }
        }
    }

    protected void testDecode_assertEquals(Object request, Object ret) throws IOException {
        testDecode_assertEquals(request, ret, null);
    }

    private void testDecode_assertEquals(Object request, Object ret, Object channelReceive) throws IOException {
        testDecode_assertEquals(null, request, ret, channelReceive);
    }

    private void testDecode_assertEquals(AbstractMockChannel channel, Object request, Object expectret, Object channelReceive) throws IOException {
        //init channel
        if (channel == null) {
            channel = getServerSideChannel(url);
        }

        byte[] buf = objectToByte(request);
        ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(buf);

        //decode
        Object obj = codec.decode(channel, buffer);
        Assert.assertEquals(expectret, obj);
        Assert.assertEquals(channelReceive, channel.getReceivedMessage());
    }

    private void testDecode_PersonWithEnterByte(byte[] enterbytes, boolean isNeedmore) throws IOException {
        //init channel
        Channel channel = getServerSideChannel(url);
        //init request string
        Person request = new Person();
        byte[] newbuf = join(objectToByte(request), enterbytes);
        ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(newbuf);

        //decode
        Object obj = codec.decode(channel, buffer);
        if (isNeedmore) {
            Assert.assertEquals(Codec2.DecodeResult.NEED_MORE_INPUT, obj);
        } else {
            Assert.assertTrue("return must string ", obj instanceof String);
        }
    }

    private void testDecode_WithExitByte(byte[] exitbytes, boolean isChannelClose) throws IOException {
        //init channel
        Channel channel = getServerSideChannel(url);
        ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(exitbytes);

        //decode
        codec.decode(channel, buffer);
        Assert.assertEquals(isChannelClose, channel.isClosed());
    }

    @Test
    public void testDecode_String_ClientSide() throws IOException {
        testDecode_assertEquals("aaa".getBytes(), "aaa", false);
    }

    @Test
    public void testDecode_BlankMessage() throws IOException {
        testDecode_assertEquals(new byte[]{}, Codec2.DecodeResult.NEED_MORE_INPUT);
    }

    @Test
    public void testDecode_String_NoEnter() throws IOException {
        testDecode_assertEquals("aaa", Codec2.DecodeResult.NEED_MORE_INPUT);
    }

    @Test
    public void testDecode_String_WithEnter() throws IOException {
        testDecode_assertEquals("aaa\n", "aaa");
    }

    @Test
    public void testDecode_String_MiddleWithEnter() throws IOException {
        testDecode_assertEquals("aaa\r\naaa", Codec2.DecodeResult.NEED_MORE_INPUT);
    }

    @Test
    public void testDecode_Person_ObjectOnly() throws IOException {
        testDecode_assertEquals(new Person(), Codec2.DecodeResult.NEED_MORE_INPUT);
    }

    @Test
    public void testDecode_Person_WithEnter() throws IOException {
        testDecode_PersonWithEnterByte(new byte[]{'\r', '\n'}, false);//windows end
        testDecode_PersonWithEnterByte(new byte[]{'\n', '\r'}, true);
        testDecode_PersonWithEnterByte(new byte[]{'\n'}, false); //linux end
        testDecode_PersonWithEnterByte(new byte[]{'\r'}, true);
        testDecode_PersonWithEnterByte(new byte[]{'\r', 100}, true);
    }

    @Test
    public void testDecode_WithExitByte() throws IOException {
        HashMap<byte[], Boolean> exitbytes = new HashMap<byte[], Boolean>();
        exitbytes.put(new byte[]{3}, true); /* Windows Ctrl+C */
        exitbytes.put(new byte[]{1, 3}, false); //must equal the bytes
        exitbytes.put(new byte[]{-1, -12, -1, -3, 6}, true); /* Linux Ctrl+C */
        exitbytes.put(new byte[]{1, -1, -12, -1, -3, 6}, false); //must equal the bytes
        exitbytes.put(new byte[]{-1, -19, -1, -3, 6}, true);  /* Linux Pause */

        for (byte[] exit : exitbytes.keySet()) {
            testDecode_WithExitByte(exit, exitbytes.get(exit));
        }
    }

    @Test
    public void testDecode_Backspace() throws IOException {
        //32 8 first add space and then add backspace.
        testDecode_assertEquals(new byte[]{'\b'}, Codec2.DecodeResult.NEED_MORE_INPUT, new String(new byte[]{32, 8}));

        // test chinese
        byte[] chineseBytes = "中".getBytes();
        byte[] request = join(chineseBytes, new byte[]{'\b'});
        testDecode_assertEquals(request, Codec2.DecodeResult.NEED_MORE_INPUT, new String(new byte[]{32, 32, 8, 8}));
        //There may be some problem handling chinese (negative number recognition). Ignoring this problem, the backspace key is only meaningfully input in a real telnet program.
        testDecode_assertEquals(new byte[]{'a', 'x', -1, 'x', '\b'}, Codec2.DecodeResult.NEED_MORE_INPUT, new String(new byte[]{32, 32, 8, 8}));
    }

    @Test(expected = IOException.class)
    public void testDecode_Backspace_WithError() throws IOException {
        url = url.addParameter(AbstractMockChannel.ERROR_WHEN_SEND, Boolean.TRUE.toString());
        testDecode_Backspace();
        url = url.removeParameter(AbstractMockChannel.ERROR_WHEN_SEND);
    }

    @Test()
    public void testDecode_History_UP() throws IOException {
        //init channel
        AbstractMockChannel channel = getServerSideChannel(url);

        testDecode_assertEquals(channel, UP, Codec2.DecodeResult.NEED_MORE_INPUT, null);

        String request1 = "aaa\n";
        Object expected1 = "aaa";
        //init history
        testDecode_assertEquals(channel, request1, expected1, null);

        testDecode_assertEquals(channel, UP, Codec2.DecodeResult.NEED_MORE_INPUT, expected1);
    }

    @Test(expected = IOException.class)
    public void testDecode_UPorDOWN_WithError() throws IOException {
        url = url.addParameter(AbstractMockChannel.ERROR_WHEN_SEND, Boolean.TRUE.toString());

        //init channel
        AbstractMockChannel channel = getServerSideChannel(url);

        testDecode_assertEquals(channel, UP, Codec2.DecodeResult.NEED_MORE_INPUT, null);

        String request1 = "aaa\n";
        Object expected1 = "aaa";
        //init history
        testDecode_assertEquals(channel, request1, expected1, null);

        testDecode_assertEquals(channel, UP, Codec2.DecodeResult.NEED_MORE_INPUT, expected1);

        url = url.removeParameter(AbstractMockChannel.ERROR_WHEN_SEND);
    }

    //=============================================================================================================================
    @Test
    public void testEncode_String_ClientSide() throws IOException {
        testEecode_assertEquals("aaa", "aaa\r\n".getBytes(), false);
    }
    
    /*@Test()
    public void testDecode_History_UP_DOWN_MULTI() throws IOException{
        AbstractMockChannel channel = getServerSideChannel(url);
        
        String request1 = "aaa\n"; 
        Object expected1 = request1.replace("\n", "");
        //init history 
        testDecode_assertEquals(channel, request1, expected1, null);
        
        String request2 = "bbb\n"; 
        Object expected2 = request2.replace("\n", "");
        //init history 
        testDecode_assertEquals(channel, request2, expected2, null);
        
        String request3 = "ccc\n"; 
        Object expected3= request3.replace("\n", "");
        //init history 
        testDecode_assertEquals(channel, request3, expected3, null);
        
        byte[] UP = new byte[] {27, 91, 65};
        byte[] DOWN = new byte[] {27, 91, 66};
        //history[aaa,bbb,ccc]
        testDecode_assertEquals(channel, UP, Codec.NEED_MORE_INPUT, expected3);
        testDecode_assertEquals(channel, DOWN, Codec.NEED_MORE_INPUT, expected3);
        testDecode_assertEquals(channel, UP, Codec.NEED_MORE_INPUT, expected2);
        testDecode_assertEquals(channel, UP, Codec.NEED_MORE_INPUT, expected1);
        testDecode_assertEquals(channel, UP, Codec.NEED_MORE_INPUT, expected1);
        testDecode_assertEquals(channel, UP, Codec.NEED_MORE_INPUT, expected1);
        testDecode_assertEquals(channel, DOWN, Codec.NEED_MORE_INPUT, expected2);
        testDecode_assertEquals(channel, DOWN, Codec.NEED_MORE_INPUT, expected3);
        testDecode_assertEquals(channel, DOWN, Codec.NEED_MORE_INPUT, expected3);
        testDecode_assertEquals(channel, DOWN, Codec.NEED_MORE_INPUT, expected3);
        testDecode_assertEquals(channel, UP, Codec.NEED_MORE_INPUT, expected2);
    }*/

    //======================================================
    public static class Person implements Serializable {
        private static final long serialVersionUID = 3362088148941547337L;
        public String name;
        public String sex;

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result + ((sex == null) ? 0 : sex.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Person other = (Person) obj;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            if (sex == null) {
                if (other.sex != null)
                    return false;
            } else if (!sex.equals(other.sex))
                return false;
            return true;
        }

    }

}
