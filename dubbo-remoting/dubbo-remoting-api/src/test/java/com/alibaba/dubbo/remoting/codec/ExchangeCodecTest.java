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
package com.alibaba.dubbo.remoting.codec;


import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.io.Bytes;
import com.alibaba.dubbo.common.io.UnsafeByteArrayOutputStream;
import com.alibaba.dubbo.common.serialize.ObjectOutput;
import com.alibaba.dubbo.common.serialize.Serialization;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.buffer.ChannelBuffer;
import com.alibaba.dubbo.remoting.buffer.ChannelBuffers;
import com.alibaba.dubbo.remoting.exchange.Request;
import com.alibaba.dubbo.remoting.exchange.Response;
import com.alibaba.dubbo.remoting.exchange.codec.ExchangeCodec;
import com.alibaba.dubbo.remoting.telnet.codec.TelnetCodec;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;

import static org.junit.Assert.fail;

/**
 * @author chao.liuc
 *         byte 16
 *         0-1 magic code
 *         2 flag
 *         8 - 1-request/0-response
 *         7 - two way
 *         6 - heartbeat
 *         1-5 serialization id
 *         3 status
 *         20 ok
 *         90 error?
 *         4-11 id (long)
 *         12 -15 datalength
 */
public class ExchangeCodecTest extends TelnetCodecTest {
    // magic header.
    private static final short MAGIC = (short) 0xdabb;
    private static final byte MAGIC_HIGH = (byte) Bytes.short2bytes(MAGIC)[0];
    private static final byte MAGIC_LOW = (byte) Bytes.short2bytes(MAGIC)[1];
    Serialization serialization = getSerialization(Constants.DEFAULT_REMOTING_SERIALIZATION);

    private static Serialization getSerialization(String name) {
        Serialization serialization = ExtensionLoader.getExtensionLoader(Serialization.class).getExtension(name);
        return serialization;
    }

    private Object decode(byte[] request) throws IOException {
        ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(request);
        AbstractMockChannel channel = getServerSideChannel(url);
        //decode
        Object obj = codec.decode(channel, buffer);
        return obj;
    }

    private byte[] getRequestBytes(Object obj, byte[] header) throws IOException {
        // encode request data.
        UnsafeByteArrayOutputStream bos = new UnsafeByteArrayOutputStream(1024);
        ObjectOutput out = serialization.serialize(url, bos);
        out.writeObject(obj);

        out.flushBuffer();
        bos.flush();
        bos.close();
        byte[] data = bos.toByteArray();
        byte[] len = Bytes.int2bytes(data.length);
        System.arraycopy(len, 0, header, 12, 4);
        byte[] request = join(header, data);
        return request;
    }

    private byte[] assemblyDataProtocol(byte[] header) {
        Person request = new Person();
        byte[] newbuf = join(header, objectToByte(request));
        return newbuf;
    }
    //===================================================================================

    @Before
    public void setUp() throws Exception {
        codec = new ExchangeCodec();
    }

    @Test
    public void test_Decode_Error_MagicNum() throws IOException {
        HashMap<byte[], Object> inputBytes = new HashMap<byte[], Object>();
        inputBytes.put(new byte[]{0}, TelnetCodec.DecodeResult.NEED_MORE_INPUT);
        inputBytes.put(new byte[]{MAGIC_HIGH, 0}, TelnetCodec.DecodeResult.NEED_MORE_INPUT);
        inputBytes.put(new byte[]{0, MAGIC_LOW}, TelnetCodec.DecodeResult.NEED_MORE_INPUT);

        for (byte[] input : inputBytes.keySet()) {
            testDecode_assertEquals(assemblyDataProtocol(input), inputBytes.get(input));
        }
    }

    @Test
    public void test_Decode_Error_Length() throws IOException {
        byte[] header = new byte[]{MAGIC_HIGH, MAGIC_LOW, 0x20, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        Person person = new Person();
        byte[] request = getRequestBytes(person, header);

        Channel channel = getServerSideChannel(url);
        byte[] baddata = new byte[]{1, 2};
        ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(join(request, baddata));
        Response obj = (Response) codec.decode(channel, buffer);
        Assert.assertEquals(person, obj.getResult());
        //only decode necessary bytes
        Assert.assertEquals(request.length, buffer.readerIndex());
    }

    @Test
    public void test_Decode_Error_Response_Object() throws IOException {
        //00000010-response/oneway/hearbeat=true |20-stats=ok|id=0|length=0
        byte[] header = new byte[]{MAGIC_HIGH, MAGIC_LOW, 0x20, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        Person person = new Person();
        byte[] request = getRequestBytes(person, header);
        //bad object
        byte[] badbytes = new byte[]{-1, -2, -3, -4, -3, -4, -3, -4, -3, -4, -3, -4};
        System.arraycopy(badbytes, 0, request, 21, badbytes.length);

        Response obj = (Response) decode(request);
        Assert.assertEquals(90, obj.getStatus());
    }

    @Test
    public void test_Decode_Check_Payload() throws IOException {
        byte[] header = new byte[]{MAGIC_HIGH, MAGIC_LOW, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
        byte[] request = assemblyDataProtocol(header);
        try {
            testDecode_assertEquals(request, TelnetCodec.DecodeResult.NEED_MORE_INPUT);
            fail();
        } catch (IOException expected) {
            Assert.assertTrue(expected.getMessage().startsWith("Data length too large: " + Bytes.bytes2int(new byte[]{1, 1, 1, 1})));
        }
    }

    @Test
    public void test_Decode_Header_Need_Readmore() throws IOException {
        byte[] header = new byte[]{MAGIC_HIGH, MAGIC_LOW, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        testDecode_assertEquals(header, TelnetCodec.DecodeResult.NEED_MORE_INPUT);
    }

    @Test
    public void test_Decode_Body_Need_Readmore() throws IOException {
        byte[] header = new byte[]{MAGIC_HIGH, MAGIC_LOW, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 'a', 'a'};
        testDecode_assertEquals(header, TelnetCodec.DecodeResult.NEED_MORE_INPUT);
    }

    @Test
    public void test_Decode_MigicCodec_Contain_ExchangeHeader() throws IOException {
        //
        byte[] header = new byte[]{0, 0, MAGIC_HIGH, MAGIC_LOW, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        Channel channel = getServerSideChannel(url);
        ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(header);
        Object obj = codec.decode(channel, buffer);
        Assert.assertEquals(TelnetCodec.DecodeResult.NEED_MORE_INPUT, obj);
        //如果telnet数据与request数据在同一个数据包中，不能因为telnet没有结尾字符而影响其他数据的接收.
        Assert.assertEquals(2, buffer.readerIndex());
    }

    @Test
    public void test_Decode_Return_Response_Person() throws IOException {
        //00000010-response/oneway/hearbeat=false/hessian |20-stats=ok|id=0|length=0
        byte[] header = new byte[]{MAGIC_HIGH, MAGIC_LOW, 2, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        Person person = new Person();
        byte[] request = getRequestBytes(person, header);

        Response obj = (Response) decode(request);
        Assert.assertEquals(20, obj.getStatus());
        Assert.assertEquals(person, obj.getResult());
        System.out.println(obj);
    }

    @Test //status输入有问题，序列化时读取信息出错.
    public void test_Decode_Return_Response_Error() throws IOException {
        byte[] header = new byte[]{MAGIC_HIGH, MAGIC_LOW, 2, 90, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        String errorString = "encode request data error ";
        byte[] request = getRequestBytes(errorString, header);
        Response obj = (Response) decode(request);
        Assert.assertEquals(90, obj.getStatus());
        Assert.assertEquals(errorString, obj.getErrorMessage());
    }

    @Test
    public void test_Decode_Return_Request_Event_Object() throws IOException {
        //|10011111|20-stats=ok|id=0|length=0
        byte[] header = new byte[]{MAGIC_HIGH, MAGIC_LOW, (byte) 0xff, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        Person person = new Person();
        byte[] request = getRequestBytes(person, header);

        Request obj = (Request) decode(request);
        Assert.assertEquals(person, obj.getData());
        Assert.assertEquals(true, obj.isTwoWay());
        Assert.assertEquals(true, obj.isEvent());
        Assert.assertEquals("2.0.0", obj.getVersion());
        System.out.println(obj);
    }

    @Test
    public void test_Decode_Return_Request_Event_String() throws IOException {
        //|10011111|20-stats=ok|id=0|length=0
        byte[] header = new byte[]{MAGIC_HIGH, MAGIC_LOW, (byte) 0xff, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        String event = Request.READONLY_EVENT;
        byte[] request = getRequestBytes(event, header);

        Request obj = (Request) decode(request);
        Assert.assertEquals(event, obj.getData());
        Assert.assertEquals(true, obj.isTwoWay());
        Assert.assertEquals(true, obj.isEvent());
        Assert.assertEquals("2.0.0", obj.getVersion());
        System.out.println(obj);
    }

    @Test
    public void test_Decode_Return_Request_Heartbeat_Object() throws IOException {
        //|10011111|20-stats=ok|id=0|length=0
        byte[] header = new byte[]{MAGIC_HIGH, MAGIC_LOW, (byte) 0xff, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        byte[] request = getRequestBytes(null, header);
        Request obj = (Request) decode(request);
        Assert.assertEquals(null, obj.getData());
        Assert.assertEquals(true, obj.isTwoWay());
        Assert.assertEquals(true, obj.isHeartbeat());
        Assert.assertEquals("2.0.0", obj.getVersion());
        System.out.println(obj);
    }

    @Test
    public void test_Decode_Return_Request_Object() throws IOException {
        //|10011111|20-stats=ok|id=0|length=0
        byte[] header = new byte[]{MAGIC_HIGH, MAGIC_LOW, (byte) 0xdf, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        Person person = new Person();
        byte[] request = getRequestBytes(person, header);

        Request obj = (Request) decode(request);
        Assert.assertEquals(person, obj.getData());
        Assert.assertEquals(true, obj.isTwoWay());
        Assert.assertEquals(false, obj.isHeartbeat());
        Assert.assertEquals("2.0.0", obj.getVersion());
        System.out.println(obj);
    }

    @Test
    public void test_Decode_Error_Request_Object() throws IOException {
        //00000010-response/oneway/hearbeat=true |20-stats=ok|id=0|length=0
        byte[] header = new byte[]{MAGIC_HIGH, MAGIC_LOW, (byte) 0xdf, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        Person person = new Person();
        byte[] request = getRequestBytes(person, header);
        //bad object
        byte[] badbytes = new byte[]{-1, -2, -3, -4, -3, -4, -3, -4, -3, -4, -3, -4};
        System.arraycopy(badbytes, 0, request, 21, badbytes.length);

        Request obj = (Request) decode(request);
        Assert.assertEquals(true, obj.isBroken());
        Assert.assertEquals(true, obj.getData() instanceof Throwable);
    }

    @Test
    public void test_Header_Response_NoSerializationFlag() throws IOException {
        //00000010-response/oneway/hearbeat=false/noset |20-stats=ok|id=0|length=0
        byte[] header = new byte[]{MAGIC_HIGH, MAGIC_LOW, 0, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        Person person = new Person();
        byte[] request = getRequestBytes(person, header);

        Response obj = (Response) decode(request);
        Assert.assertEquals(20, obj.getStatus());
        Assert.assertEquals(person, obj.getResult());
        System.out.println(obj);
    }

    @Test
    public void test_Header_Response_Heartbeat() throws IOException {
        //00000010-response/oneway/hearbeat=true |20-stats=ok|id=0|length=0
        byte[] header = new byte[]{MAGIC_HIGH, MAGIC_LOW, 0x20, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        Person person = new Person();
        byte[] request = getRequestBytes(person, header);

        Response obj = (Response) decode(request);
        Assert.assertEquals(20, obj.getStatus());
        Assert.assertEquals(person, obj.getResult());
        System.out.println(obj);
    }

    @Test
    public void test_Encode_Request() throws IOException {
        ChannelBuffer encodeBuffer = ChannelBuffers.dynamicBuffer(2014);
        Channel channel = getCliendSideChannel(url);
        Request request = new Request();
        Person person = new Person();
        request.setData(person);

        codec.encode(channel, encodeBuffer, request);

        //encode resault check need decode
        byte[] data = new byte[encodeBuffer.writerIndex()];
        encodeBuffer.readBytes(data);
        ChannelBuffer decodeBuffer = ChannelBuffers.wrappedBuffer(data);
        Request obj = (Request) codec.decode(channel, decodeBuffer);
        Assert.assertEquals(request.isBroken(), obj.isBroken());
        Assert.assertEquals(request.isHeartbeat(), obj.isHeartbeat());
        Assert.assertEquals(request.isTwoWay(), obj.isTwoWay());
        Assert.assertEquals(person, obj.getData());
    }

    @Test
    public void test_Encode_Response() throws IOException {
        ChannelBuffer encodeBuffer = ChannelBuffers.dynamicBuffer(1024);
        Channel channel = getCliendSideChannel(url);
        Response response = new Response();
        response.setHeartbeat(true);
        response.setId(1001l);
        response.setStatus((byte) 20);
        response.setVersion("11");
        Person person = new Person();
        response.setResult(person);

        codec.encode(channel, encodeBuffer, response);
        byte[] data = new byte[encodeBuffer.writerIndex()];
        encodeBuffer.readBytes(data);

        //encode resault check need decode 
        ChannelBuffer decodeBuffer = ChannelBuffers.wrappedBuffer(data);
        Response obj = (Response) codec.decode(channel, decodeBuffer);

        Assert.assertEquals(response.getId(), obj.getId());
        Assert.assertEquals(response.getStatus(), obj.getStatus());
        Assert.assertEquals(response.isHeartbeat(), obj.isHeartbeat());
        Assert.assertEquals(person, obj.getResult());
        // encode response verson ?? 
//        Assert.assertEquals(response.getVersion(), obj.getVersion());

    }

    @Test
    public void test_Encode_Error_Response() throws IOException {
        ChannelBuffer encodeBuffer = ChannelBuffers.dynamicBuffer(1024);
        Channel channel = getCliendSideChannel(url);
        Response response = new Response();
        response.setHeartbeat(true);
        response.setId(1001l);
        response.setStatus((byte) 10);
        response.setVersion("11");
        String badString = "bad";
        response.setErrorMessage(badString);
        Person person = new Person();
        response.setResult(person);

        codec.encode(channel, encodeBuffer, response);
        byte[] data = new byte[encodeBuffer.writerIndex()];
        encodeBuffer.readBytes(data);

        //encode resault check need decode 
        ChannelBuffer decodeBuffer = ChannelBuffers.wrappedBuffer(data);
        Response obj = (Response) codec.decode(channel, decodeBuffer);
        Assert.assertEquals(response.getId(), obj.getId());
        Assert.assertEquals(response.getStatus(), obj.getStatus());
        Assert.assertEquals(response.isHeartbeat(), obj.isHeartbeat());
        Assert.assertEquals(badString, obj.getErrorMessage());
        Assert.assertEquals(null, obj.getResult());
//        Assert.assertEquals(response.getVersion(), obj.getVersion());
    }

    // http://code.alibabatech.com/jira/browse/DUBBO-392
    @Test
    public void testMessageLengthGreaterThanMessageActualLength() throws Exception {
        Channel channel = getCliendSideChannel(url);
        Request request = new Request(1L);
        request.setVersion("2.0.0");
        Date date = new Date();
        request.setData(date);
        ChannelBuffer encodeBuffer = ChannelBuffers.dynamicBuffer(1024);
        codec.encode(channel, encodeBuffer, request);
        byte[] bytes = new byte[encodeBuffer.writerIndex()];
        encodeBuffer.readBytes(bytes);
        int len = Bytes.bytes2int(bytes, 12);
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        out.write(bytes, 0, 12);
        /*
         * 填充长度不能低于256，hessian每次默认会从流中读取256个byte.
         * 参见 Hessian2Input.readBuffer
         */
        int padding = 512;
        out.write(Bytes.int2bytes(len + padding));
        out.write(bytes, 16, bytes.length - 16);
        for (int i = 0; i < padding; i++) {
            out.write(1);
        }
        out.write(bytes);
        /* request|1111...|request */
        ChannelBuffer decodeBuffer = ChannelBuffers.wrappedBuffer(out.toByteArray());
        Request decodedRequest = (Request) codec.decode(channel, decodeBuffer);
        Assert.assertTrue(date.equals(decodedRequest.getData()));
        Assert.assertEquals(bytes.length + padding, decodeBuffer.readerIndex());
        decodedRequest = (Request) codec.decode(channel, decodeBuffer);
        Assert.assertTrue(date.equals(decodedRequest.getData()));
    }

    @Test
    public void testMessageLengthExceedPayloadLimitWhenEncode() throws Exception {
        Request request = new Request(1L);
        request.setData("hello");
        ChannelBuffer encodeBuffer = ChannelBuffers.dynamicBuffer(512);
        AbstractMockChannel channel = getCliendSideChannel(url.addParameter(Constants.PAYLOAD_KEY, 4));
        try {
            codec.encode(channel, encodeBuffer, request);
            Assert.fail();
        } catch (IOException e) {
            Assert.assertTrue(e.getMessage().startsWith("Data length too large: " + 6));
        }

        Response response = new Response(1L);
        response.setResult("hello");
        encodeBuffer = ChannelBuffers.dynamicBuffer(512);
        channel = getServerSideChannel(url.addParameter(Constants.PAYLOAD_KEY, 4));
        codec.encode(channel, encodeBuffer, response);
        Assert.assertTrue(channel.getReceivedMessage() instanceof Response);
        Response receiveMessage = (Response) channel.getReceivedMessage();
        Assert.assertEquals(Response.BAD_RESPONSE, receiveMessage.getStatus());
        Assert.assertTrue(receiveMessage.getErrorMessage().contains("Data length too large: "));
    }
}