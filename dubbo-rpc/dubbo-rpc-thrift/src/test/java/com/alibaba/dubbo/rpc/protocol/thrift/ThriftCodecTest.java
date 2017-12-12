/**
 * File Created at 2011-12-05
 * $Id$
 * <p>
 * Copyright 2008 Alibaba.com Croporation Limited.
 * All rights reserved.
 * <p>
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.rpc.protocol.thrift;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.buffer.ChannelBuffer;
import com.alibaba.dubbo.remoting.buffer.ChannelBuffers;
import com.alibaba.dubbo.remoting.exchange.Request;
import com.alibaba.dubbo.remoting.exchange.Response;
import com.alibaba.dubbo.remoting.exchange.support.DefaultFuture;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.RpcResult;
import com.alibaba.dubbo.rpc.gen.thrift.Demo;
import com.alibaba.dubbo.rpc.protocol.thrift.io.RandomAccessByteArrayOutputStream;

import org.apache.thrift.TApplicationException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TMessageType;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TIOStreamTransport;
import org.apache.thrift.transport.TTransport;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayInputStream;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">gang.lvg</a>
 */
@Ignore
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
        Assert.assertEquals(ThriftCodec.MAGIC, protocol.readI16());

        // message length
        int messageLength = protocol.readI32();
        Assert.assertEquals(messageLength + 4, bytes.length);

        // header length
        short headerLength = protocol.readI16();
        // version
        Assert.assertEquals(ThriftCodec.VERSION, protocol.readByte());
        // service name
        Assert.assertEquals(Demo.Iface.class.getName(), protocol.readString());
        // dubbo request id
        Assert.assertEquals(request.getId(), protocol.readI64());

        // test message header length
        if (bis.markSupported()) {
            bis.reset();
            bis.skip(headerLength);
        }

        TMessage message = protocol.readMessageBegin();

        Demo.echoString_args args = new Demo.echoString_args();

        args.read(protocol);

        protocol.readMessageEnd();

        Assert.assertEquals("echoString", message.name);

        Assert.assertEquals(TMessageType.CALL, message.type);

        Assert.assertEquals("Hello, World!", args.getArg());

    }

    @Test
    public void testDecodeReplyResponse() throws Exception {

        URL url = URL.valueOf(ThriftProtocol.NAME + "://127.0.0.1:40880/" + Demo.Iface.class.getName());

        Channel channel = new MockedChannel(url);

        RandomAccessByteArrayOutputStream bos = new RandomAccessByteArrayOutputStream(128);

        Request request = createRequest();

        DefaultFuture future = new DefaultFuture(channel, request, 10);

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

        Assert.assertNotNull(obj);

        Assert.assertEquals(true, obj instanceof Response);

        Response response = (Response) obj;

        Assert.assertEquals(request.getId(), response.getId());

        Assert.assertTrue(response.getResult() instanceof RpcResult);

        RpcResult result = (RpcResult) response.getResult();

        Assert.assertTrue(result.getResult() instanceof String);

        Assert.assertEquals(methodResult.success, result.getResult());

    }

    @Test
    public void testDecodeExceptionResponse() throws Exception {

        URL url = URL.valueOf(ThriftProtocol.NAME + "://127.0.0.1:40880/" + Demo.class.getName());

        Channel channel = new MockedChannel(url);

        RandomAccessByteArrayOutputStream bos = new RandomAccessByteArrayOutputStream(128);

        Request request = createRequest();

        DefaultFuture future = new DefaultFuture(channel, request, 10);

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

        Assert.assertNotNull(obj);

        Assert.assertTrue(obj instanceof Response);

        Response response = (Response) obj;

        Assert.assertTrue(response.getResult() instanceof RpcResult);

        RpcResult result = (RpcResult) response.getResult();

        Assert.assertTrue(result.hasException());

        Assert.assertTrue(result.getException() instanceof RpcException);

    }

    @Test
    public void testEncodeReplyResponse() throws Exception {

        URL url = URL.valueOf(ThriftProtocol.NAME + "://127.0.0.1:40880/" + Demo.Iface.class.getName());

        Channel channel = new MockedChannel(url);

        Request request = createRequest();

        RpcResult rpcResult = new RpcResult();
        rpcResult.setResult("Hello, World!");

        Response response = new Response();
        response.setResult(rpcResult);
        response.setId(request.getId());
        ChannelBuffer bos = ChannelBuffers.dynamicBuffer(1024);

        ThriftCodec.RequestData rd = ThriftCodec.RequestData.create(
                ThriftCodec.getSeqId(), Demo.Iface.class.getName(), "echoString");
        ThriftCodec.cachedRequest.putIfAbsent(request.getId(), rd);
        codec.encode(channel, bos, response);

        byte[] buf = new byte[bos.writerIndex() - 4];
        System.arraycopy(bos.array(), 4, buf, 0, bos.writerIndex() - 4);

        ByteArrayInputStream bis = new ByteArrayInputStream(buf);

        if (bis.markSupported()) {
            bis.mark(0);
        }

        TIOStreamTransport transport = new TIOStreamTransport(bis);
        TBinaryProtocol protocol = new TBinaryProtocol(transport);

        Assert.assertEquals(ThriftCodec.MAGIC, protocol.readI16());
        Assert.assertEquals(protocol.readI32() + 4, bos.writerIndex());
        int headerLength = protocol.readI16();

        Assert.assertEquals(ThriftCodec.VERSION, protocol.readByte());
        Assert.assertEquals(Demo.Iface.class.getName(), protocol.readString());
        Assert.assertEquals(request.getId(), protocol.readI64());

        if (bis.markSupported()) {
            bis.reset();
            bis.skip(headerLength);
        }

        TMessage message = protocol.readMessageBegin();
        Assert.assertEquals("echoString", message.name);
        Assert.assertEquals(TMessageType.REPLY, message.type);
        Assert.assertEquals(ThriftCodec.getSeqId(), message.seqid);
        Demo.echoString_result result = new Demo.echoString_result();
        result.read(protocol);
        protocol.readMessageEnd();

        Assert.assertEquals(rpcResult.getValue(), result.getSuccess());
    }

    @Test
    public void testEncodeExceptionResponse() throws Exception {

        URL url = URL.valueOf(ThriftProtocol.NAME + "://127.0.0.1:40880/" + Demo.Iface.class.getName());

        Channel channel = new MockedChannel(url);

        Request request = createRequest();

        RpcResult rpcResult = new RpcResult();
        String exceptionMessage = "failed";
        rpcResult.setException(new RuntimeException(exceptionMessage));

        Response response = new Response();
        response.setResult(rpcResult);
        response.setId(request.getId());
        ChannelBuffer bos = ChannelBuffers.dynamicBuffer(1024);

        ThriftCodec.RequestData rd = ThriftCodec.RequestData.create(
                ThriftCodec.getSeqId(), Demo.Iface.class.getName(), "echoString");
        ThriftCodec.cachedRequest.put(request.getId(), rd);
        codec.encode(channel, bos, response);

        byte[] buf = new byte[bos.writerIndex() - 4];
        System.arraycopy(bos.array(), 4, buf, 0, bos.writerIndex() - 4);
        ByteArrayInputStream bis = new ByteArrayInputStream(buf);

        if (bis.markSupported()) {
            bis.mark(0);
        }

        TIOStreamTransport transport = new TIOStreamTransport(bis);
        TBinaryProtocol protocol = new TBinaryProtocol(transport);

        Assert.assertEquals(ThriftCodec.MAGIC, protocol.readI16());
        Assert.assertEquals(protocol.readI32() + 4, bos.writerIndex());
        int headerLength = protocol.readI16();

        Assert.assertEquals(ThriftCodec.VERSION, protocol.readByte());
        Assert.assertEquals(Demo.Iface.class.getName(), protocol.readString());
        Assert.assertEquals(request.getId(), protocol.readI64());

        if (bis.markSupported()) {
            bis.reset();
            bis.skip(headerLength);
        }

        TMessage message = protocol.readMessageBegin();
        Assert.assertEquals("echoString", message.name);
        Assert.assertEquals(TMessageType.EXCEPTION, message.type);
        Assert.assertEquals(ThriftCodec.getSeqId(), message.seqid);
        TApplicationException exception = TApplicationException.read(protocol);
        protocol.readMessageEnd();

        Assert.assertEquals(exceptionMessage, exception.getMessage());

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
                        .getAttachment(Constants.INTERFACE_KEY));
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

        Assert.assertTrue(obj instanceof Request);

        obj = ((Request) obj).getData();

        Assert.assertTrue(obj instanceof RpcInvocation);

        RpcInvocation invocation = (RpcInvocation) obj;

        Assert.assertEquals("echoString", invocation.getMethodName());
        Assert.assertArrayEquals(new Class[]{String.class}, invocation.getParameterTypes());
        Assert.assertArrayEquals(new Object[]{args.getArg()}, invocation.getArguments());

    }

    private Request createRequest() {

        RpcInvocation invocation = new RpcInvocation();

        invocation.setMethodName("echoString");

        invocation.setArguments(new Object[]{"Hello, World!"});

        invocation.setParameterTypes(new Class<?>[]{String.class});

        invocation.setAttachment(Constants.INTERFACE_KEY, Demo.Iface.class.getName());

        Request request = new Request(1L);

        request.setData(invocation);

        return request;

    }

}
