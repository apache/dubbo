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
package org.apache.dubbo.rpc.protocol.thrift;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.buffer.ChannelBuffer;
import org.apache.dubbo.remoting.buffer.ChannelBuffers;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.remoting.exchange.support.DefaultFuture;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.gen.thrift.Demo;
import org.apache.dubbo.rpc.protocol.thrift.io.RandomAccessByteArrayOutputStream;

import org.apache.thrift.TApplicationException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TMessageType;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TIOStreamTransport;
import org.apache.thrift.transport.TTransport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;

public class ThriftCodecTest {

    private ThriftCodec codec = new ThriftCodec();
    private Channel channel = new MockedChannel(URL.valueOf("thrift://127.0.0.1"));

    static byte[] encodeFrame(byte[] content) {
        byte[] result = new byte[4 + content.length];
        TFramedTransport.encodeFrameSize(content.length, result);
        System.arraycopy(content, 0, result, 4, content.length);
        return result;
    }

    @Test
    public void testEncodeRequest() throws Exception {

        Request request = createRequest();

        ChannelBuffer output = ChannelBuffers.dynamicBuffer(1024);

        codec.encode(channel, output, request);

        byte[] bytes = new byte[output.readableBytes()];
        output.readBytes(bytes);

        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);

        TTransport transport = new TIOStreamTransport(bis);

        TBinaryProtocol protocol = new TBinaryProtocol(transport);

        // frame
        byte[] length = new byte[4];
        transport.read(length, 0, 4);

        if (bis.markSupported()) {
            bis.mark(0);
        }

        // magic
        Assertions.assertEquals(ThriftCodec.MAGIC, protocol.readI16());

        // message length
        int messageLength = protocol.readI32();
        Assertions.assertEquals(messageLength + 4, bytes.length);

        // header length
        short headerLength = protocol.readI16();
        // version
        Assertions.assertEquals(ThriftCodec.VERSION, protocol.readByte());
        // service name
        Assertions.assertEquals(Demo.Iface.class.getName(), protocol.readString());
        // path
        Assertions.assertEquals(Demo.Iface.class.getName(), protocol.readString());
        // dubbo request id
        Assertions.assertEquals(request.getId(), protocol.readI64());

        // test message header length
        if (bis.markSupported()) {
            bis.reset();
            bis.skip(headerLength);
        }

        TMessage message = protocol.readMessageBegin();

        Demo.echoString_args args = new Demo.echoString_args();

        args.read(protocol);

        protocol.readMessageEnd();

        Assertions.assertEquals("echoString", message.name);

        Assertions.assertEquals(TMessageType.CALL, message.type);

        Assertions.assertEquals("Hello, World!", args.getArg());

    }

    @Test
    public void testDecodeReplyResponse() throws Exception {

        URL url = URL.valueOf(ThriftProtocol.NAME + "://127.0.0.1:40880/" + Demo.Iface.class.getName());

        Channel channel = new MockedChannel(url);

        RandomAccessByteArrayOutputStream bos = new RandomAccessByteArrayOutputStream(128);

        Request request = createRequest();

        DefaultFuture future = DefaultFuture.newFuture(channel, request, 10);

        TMessage message = new TMessage("echoString", TMessageType.REPLY, ThriftCodec.getSeqId());

        Demo.echoString_result methodResult = new Demo.echoString_result();

        methodResult.success = "Hello, World!";

        TTransport transport = new TIOStreamTransport(bos);

        TBinaryProtocol protocol = new TBinaryProtocol(transport);

        int messageLength, headerLength;
        // prepare
        protocol.writeI16(ThriftCodec.MAGIC);
        protocol.writeI32(Integer.MAX_VALUE);
        protocol.writeI16(Short.MAX_VALUE);
        protocol.writeByte(ThriftCodec.VERSION);
        protocol.writeString(Demo.Iface.class.getName());
        // path
        protocol.writeString(Demo.Iface.class.getName());
        protocol.writeI64(request.getId());
        protocol.getTransport().flush();
        headerLength = bos.size();

        protocol.writeMessageBegin(message);
        methodResult.write(protocol);
        protocol.writeMessageEnd();
        protocol.getTransport().flush();
        int oldIndex = messageLength = bos.size();

        try {
            bos.setWriteIndex(ThriftCodec.MESSAGE_LENGTH_INDEX);
            protocol.writeI32(messageLength);
            bos.setWriteIndex(ThriftCodec.MESSAGE_HEADER_LENGTH_INDEX);
            protocol.writeI16((short) (0xffff & headerLength));
        } finally {
            bos.setWriteIndex(oldIndex);
        }
        // prepare

        byte[] buf = new byte[4 + bos.size()];
        System.arraycopy(bos.toByteArray(), 0, buf, 4, bos.size());

        ChannelBuffer bis = ChannelBuffers.wrappedBuffer(buf);

        Object obj = codec.decode((Channel) null, bis);

        Assertions.assertNotNull(obj);

        Assertions.assertEquals(true, obj instanceof Response);

        Response response = (Response) obj;

        Assertions.assertEquals(request.getId(), response.getId());

        Assertions.assertTrue(response.getResult() instanceof AppResponse);

        AppResponse result = (AppResponse) response.getResult();

        Assertions.assertTrue(result.getValue() instanceof String);

        Assertions.assertEquals(methodResult.success, result.getValue());

    }

    @Test
    public void testDecodeExceptionResponse() throws Exception {

        URL url = URL.valueOf(ThriftProtocol.NAME + "://127.0.0.1:40880/" + Demo.class.getName());

        Channel channel = new MockedChannel(url);

        RandomAccessByteArrayOutputStream bos = new RandomAccessByteArrayOutputStream(128);

        Request request = createRequest();

        DefaultFuture future = DefaultFuture.newFuture(channel, request, 10);

        TMessage message = new TMessage("echoString", TMessageType.EXCEPTION, ThriftCodec.getSeqId());

        TTransport transport = new TIOStreamTransport(bos);

        TBinaryProtocol protocol = new TBinaryProtocol(transport);

        TApplicationException exception = new TApplicationException();

        int messageLength, headerLength;
        // prepare
        protocol.writeI16(ThriftCodec.MAGIC);
        protocol.writeI32(Integer.MAX_VALUE);
        protocol.writeI16(Short.MAX_VALUE);
        protocol.writeByte(ThriftCodec.VERSION);
        protocol.writeString(Demo.class.getName());
        // path
        protocol.writeString(Demo.class.getName());
        protocol.writeI64(request.getId());
        protocol.getTransport().flush();
        headerLength = bos.size();

        protocol.writeMessageBegin(message);
        exception.write(protocol);
        protocol.writeMessageEnd();
        protocol.getTransport().flush();
        int oldIndex = messageLength = bos.size();

        try {
            bos.setWriteIndex(ThriftCodec.MESSAGE_LENGTH_INDEX);
            protocol.writeI32(messageLength);
            bos.setWriteIndex(ThriftCodec.MESSAGE_HEADER_LENGTH_INDEX);
            protocol.writeI16((short) (0xffff & headerLength));
        } finally {
            bos.setWriteIndex(oldIndex);
        }
        // prepare

        ChannelBuffer bis = ChannelBuffers.wrappedBuffer(encodeFrame(bos.toByteArray()));

        Object obj = codec.decode((Channel) null, bis);

        Assertions.assertNotNull(obj);

        Assertions.assertTrue(obj instanceof Response);

        Response response = (Response) obj;

        Assertions.assertTrue(response.getResult() instanceof AppResponse);

        AppResponse result = (AppResponse) response.getResult();

        Assertions.assertTrue(result.hasException());

        Assertions.assertTrue(result.getException() instanceof RpcException);

    }

    @Test
    public void testEncodeReplyResponse() throws Exception {

        URL url = URL.valueOf(ThriftProtocol.NAME + "://127.0.0.1:40880/" + Demo.Iface.class.getName());

        Channel channel = new MockedChannel(url);

        Request request = createRequest();

        AppResponse appResponse = new AppResponse();
        appResponse.setValue("Hello, World!");

        Response response = new Response();
        response.setResult(appResponse);
        response.setId(request.getId());
        ChannelBuffer bos = ChannelBuffers.dynamicBuffer(1024);

        ThriftCodec.RequestData rd = ThriftCodec.RequestData.create(
                ThriftCodec.getSeqId(), Demo.Iface.class.getName(), "echoString");
        ThriftCodec.CACHED_REQUEST.putIfAbsent(request.getId(), rd);
        codec.encode(channel, bos, response);

        byte[] buf = new byte[bos.writerIndex() - 4];
        System.arraycopy(bos.array(), 4, buf, 0, bos.writerIndex() - 4);

        ByteArrayInputStream bis = new ByteArrayInputStream(buf);

        if (bis.markSupported()) {
            bis.mark(0);
        }

        TIOStreamTransport transport = new TIOStreamTransport(bis);
        TBinaryProtocol protocol = new TBinaryProtocol(transport);

        Assertions.assertEquals(ThriftCodec.MAGIC, protocol.readI16());
        Assertions.assertEquals(protocol.readI32() + 4, bos.writerIndex());
        int headerLength = protocol.readI16();

        Assertions.assertEquals(ThriftCodec.VERSION, protocol.readByte());
        Assertions.assertEquals(Demo.Iface.class.getName(), protocol.readString());
        Assertions.assertEquals(request.getId(), protocol.readI64());

        if (bis.markSupported()) {
            bis.reset();
            bis.skip(headerLength);
        }

        TMessage message = protocol.readMessageBegin();
        Assertions.assertEquals("echoString", message.name);
        Assertions.assertEquals(TMessageType.REPLY, message.type);
        //Assertions.assertEquals(ThriftCodec.getSeqId(), message.seqid);
        Demo.echoString_result result = new Demo.echoString_result();
        result.read(protocol);
        protocol.readMessageEnd();

        Assertions.assertEquals(appResponse.getValue(), result.getSuccess());
    }

    @Test
    public void testEncodeExceptionResponse() throws Exception {

        URL url = URL.valueOf(ThriftProtocol.NAME + "://127.0.0.1:40880/" + Demo.Iface.class.getName());

        Channel channel = new MockedChannel(url);

        Request request = createRequest();

        AppResponse appResponse = new AppResponse();
        String exceptionMessage = "failed";
        appResponse.setException(new RuntimeException(exceptionMessage));

        Response response = new Response();
        response.setResult(appResponse);
        response.setId(request.getId());
        ChannelBuffer bos = ChannelBuffers.dynamicBuffer(1024);

        ThriftCodec.RequestData rd = ThriftCodec.RequestData.create(
                ThriftCodec.getSeqId(), Demo.Iface.class.getName(), "echoString");
        ThriftCodec.CACHED_REQUEST.put(request.getId(), rd);
        codec.encode(channel, bos, response);

        byte[] buf = new byte[bos.writerIndex() - 4];
        System.arraycopy(bos.array(), 4, buf, 0, bos.writerIndex() - 4);
        ByteArrayInputStream bis = new ByteArrayInputStream(buf);

        if (bis.markSupported()) {
            bis.mark(0);
        }

        TIOStreamTransport transport = new TIOStreamTransport(bis);
        TBinaryProtocol protocol = new TBinaryProtocol(transport);

        Assertions.assertEquals(ThriftCodec.MAGIC, protocol.readI16());
        Assertions.assertEquals(protocol.readI32() + 4, bos.writerIndex());
        int headerLength = protocol.readI16();

        Assertions.assertEquals(ThriftCodec.VERSION, protocol.readByte());
        Assertions.assertEquals(Demo.Iface.class.getName(), protocol.readString());
        Assertions.assertEquals(request.getId(), protocol.readI64());

        if (bis.markSupported()) {
            bis.reset();
            bis.skip(headerLength);
        }

        TMessage message = protocol.readMessageBegin();
        Assertions.assertEquals("echoString", message.name);
        Assertions.assertEquals(TMessageType.EXCEPTION, message.type);
        Assertions.assertEquals(ThriftCodec.getSeqId(), message.seqid);
        TApplicationException exception = TApplicationException.readFrom(protocol);
        protocol.readMessageEnd();

        Assertions.assertEquals(exceptionMessage, exception.getMessage());

    }

    @Test
    public void testDecodeRequest() throws Exception {
        Request request = createRequest();
        // encode
        RandomAccessByteArrayOutputStream bos = new RandomAccessByteArrayOutputStream(1024);

        TIOStreamTransport transport = new TIOStreamTransport(bos);

        TBinaryProtocol protocol = new TBinaryProtocol(transport);

        int messageLength, headerLength;

        protocol.writeI16(ThriftCodec.MAGIC);
        protocol.writeI32(Integer.MAX_VALUE);
        protocol.writeI16(Short.MAX_VALUE);
        protocol.writeByte(ThriftCodec.VERSION);
        protocol.writeString(
                ((RpcInvocation) request.getData())
                        .getAttachment(INTERFACE_KEY));
        protocol.writeString(
                ((RpcInvocation) request.getData())
                        .getAttachment(PATH_KEY));
        protocol.writeI64(request.getId());
        protocol.getTransport().flush();
        headerLength = bos.size();

        Demo.echoString_args args = new Demo.echoString_args();
        args.setArg("Hell, World!");

        TMessage message = new TMessage("echoString", TMessageType.CALL, ThriftCodec.getSeqId());

        protocol.writeMessageBegin(message);
        args.write(protocol);
        protocol.writeMessageEnd();
        protocol.getTransport().flush();
        int oldIndex = messageLength = bos.size();

        try {
            bos.setWriteIndex(ThriftCodec.MESSAGE_HEADER_LENGTH_INDEX);
            protocol.writeI16((short) (0xffff & headerLength));
            bos.setWriteIndex(ThriftCodec.MESSAGE_LENGTH_INDEX);
            protocol.writeI32(messageLength);
        } finally {
            bos.setWriteIndex(oldIndex);
        }

        Object obj = codec.decode((Channel) null, ChannelBuffers.wrappedBuffer(
                encodeFrame(bos.toByteArray())));

        Assertions.assertTrue(obj instanceof Request);

        obj = ((Request) obj).getData();

        Assertions.assertTrue(obj instanceof RpcInvocation);

        RpcInvocation invocation = (RpcInvocation) obj;

        Assertions.assertEquals("echoString", invocation.getMethodName());
        Assertions.assertArrayEquals(new Class[]{String.class}, invocation.getParameterTypes());
        Assertions.assertArrayEquals(new Object[]{args.getArg()}, invocation.getArguments());

    }

    private Request createRequest() {

        RpcInvocation invocation = new RpcInvocation();

        invocation.setMethodName("echoString");

        invocation.setArguments(new Object[]{"Hello, World!"});

        invocation.setParameterTypes(new Class<?>[]{String.class});

        invocation.setAttachment(INTERFACE_KEY, Demo.Iface.class.getName());
        invocation.setAttachment(PATH_KEY, Demo.Iface.class.getName());

        Request request = new Request(1L);

        request.setData(invocation);

        return request;

    }

}
