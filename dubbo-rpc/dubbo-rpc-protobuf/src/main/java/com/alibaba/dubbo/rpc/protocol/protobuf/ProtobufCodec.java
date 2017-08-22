/**
 * Copyright (c) 2015 Illuminate inc.
 *
 * * Licensed under the Apache License, Version 2.0 (the "License");
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
package com.alibaba.dubbo.rpc.protocol.protobuf;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.io.StreamUtils;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.Codec2;
import com.alibaba.dubbo.remoting.buffer.ChannelBuffer;
import com.alibaba.dubbo.remoting.buffer.ChannelBufferInputStream;
import com.alibaba.dubbo.remoting.buffer.ChannelBufferOutputStream;
import com.alibaba.dubbo.remoting.exchange.Request;
import com.alibaba.dubbo.remoting.exchange.Response;
import com.alibaba.dubbo.remoting.exchange.codec.ExchangeCodec;
import com.alibaba.dubbo.remoting.exchange.support.DefaultFuture;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.RpcResult;
import com.alibaba.dubbo.rpc.protocol.protobuf.ProtobufMessage.PackageType;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * 类名称:ProtobufCodec 类描述: 创建人:xichen 修改人:xichen 修改时间:2015年12月20日-下午10:13:46 修改备注:
 */
public class ProtobufCodec implements Codec2 {

  private static final Logger logger = LoggerFactory.getLogger(ExchangeCodec.class);

  // codec name
  public static final String NAME = "protobuf";

  // header length.
  protected static final int HEADER_LENGTH = 4;

  public void encode(Channel channel, ChannelBuffer buffer, Object message) throws IOException {
    if (message instanceof Request) {
      // 处理请求
      Request request = (Request) message;
      if (request.isHeartbeat()) {
        // 心跳逻辑
        encodeHeartbeat(channel, buffer, request);
      } else {
        // 调用逻辑
        encodeRequest(channel, buffer, request);
      }
    } else if (message instanceof Response) {
      // 处理回包
      encodeResponse(channel, buffer, (Response) message);
    } else {
      throw new UnsupportedOperationException(new StringBuilder(32).append("protobuf codec only support encode ").append(Request.class.getName()).append(" and ")
          .append(Response.class.getName()).toString());
    }
  }

  private void encodeHeartbeat(Channel channel, ChannelBuffer buffer, Request request) throws IOException {
    // 打包消息体
    ProtobufMessage.RpcMessage.Builder rpcMessageBuilder = ProtobufMessage.RpcMessage.newBuilder();
    rpcMessageBuilder.setType(ProtobufMessage.PackageType.HEARTBEAT);
    rpcMessageBuilder.setIdentify(0);
    rpcMessageBuilder.setTimestamp(System.currentTimeMillis());
    ProtobufMessage.RpcMessage rpcMessage = rpcMessageBuilder.build();

    // 编码消息体
    encodeMessage(channel, buffer, rpcMessage);
  }

  private void encodeRequest(Channel channel, ChannelBuffer buffer, Request request) throws IOException {

    RpcInvocation inv = (RpcInvocation) request.getData();

    long seqID = request.getId();

    String serviceName = inv.getAttachment(Constants.INTERFACE_KEY);
    String methodName = inv.getMethodName();
    if (StringUtils.isEmpty(serviceName)) {
      throw new IllegalArgumentException(new StringBuilder(32).append("Could not find service name in attachment with key ").append(Constants.INTERFACE_KEY).toString());
    }

    // 检查参数个数
    if (inv.getArguments().length != ProtobufConstants.METHOD_ARGUMENTS || inv.getParameterTypes().length != ProtobufConstants.METHOD_ARGUMENTS) {
      throw new IllegalArgumentException("encodeRequest getArguments length:" + inv.getArguments().length + " errors");
    }
    // 检查参数类型
    if (inv.getParameterTypes()[0] != com.google.protobuf.RpcController.class) {
      throw new IllegalArgumentException("encodeRequest getArguments class:" + inv.getParameterTypes()[0] + " errors");
    }
    if (inv.getParameterTypes()[1] != ProtobufServices.getInstance().getRequestClass(serviceName, methodName)) {
      throw new IllegalArgumentException("encodeRequest getArguments class:" + inv.getParameterTypes()[1] + " errors");
    }

    // 获得请求参数实例
    com.google.protobuf.Message requestMessage = (com.google.protobuf.Message) inv.getArguments()[1];

    // 打包请求体
    ProtobufMessage.Request.Builder rpcRequestBuilder = ProtobufMessage.Request.newBuilder();
    rpcRequestBuilder.setIdentify(seqID);
    rpcRequestBuilder.setMethod(methodName);
    rpcRequestBuilder.setService(serviceName);
    rpcRequestBuilder.setRequest(requestMessage.toByteString());
    ProtobufMessage.Request rpcRequest = rpcRequestBuilder.build();

    // 是否需要回报
    ProtobufMessage.PackageType mType = request.isTwoWay() ? ProtobufMessage.PackageType.REQUEST : ProtobufMessage.PackageType.PUSH;

    // 打包消息体
    ProtobufMessage.RpcMessage.Builder rpcMessageBuilder = ProtobufMessage.RpcMessage.newBuilder();
    rpcMessageBuilder.setType(mType);
    rpcMessageBuilder.setIdentify(seqID);
    rpcMessageBuilder.setTimestamp(System.currentTimeMillis());
    rpcMessageBuilder.setSerialized(rpcRequest.toByteString());
    ProtobufMessage.RpcMessage rpcMessage = rpcMessageBuilder.build();

    // 编码消息体
    encodeMessage(channel, buffer, rpcMessage);
  }

  private void encodeResponse(Channel channel, ChannelBuffer buffer, Response response) throws IOException {

    RpcResult result = (RpcResult) response.getResult();

    long seqID = response.getId();

    // 获取请求返回结构体
    com.google.protobuf.Message responseMessage = (com.google.protobuf.Message) result.getValue();

    // 打包返回体
    ProtobufMessage.Response.Builder rpcResponseBuilder = ProtobufMessage.Response.newBuilder();
    rpcResponseBuilder.setResult(ProtobufConstants.RESULT_OK);
    rpcResponseBuilder.setResponse(responseMessage.toByteString());
    ProtobufMessage.Response rpcResponse = rpcResponseBuilder.build();

    // 打包消息体
    ProtobufMessage.RpcMessage.Builder rpcMessageBuilder = ProtobufMessage.RpcMessage.newBuilder();
    rpcMessageBuilder.setIdentify(seqID);
    rpcMessageBuilder.setTimestamp(System.currentTimeMillis());
    rpcMessageBuilder.setType(ProtobufMessage.PackageType.RESPONSE);
    rpcMessageBuilder.setSerialized(rpcResponse.toByteString());
    ProtobufMessage.RpcMessage rpcMessage = rpcMessageBuilder.build();

    // 编码消息体
    encodeMessage(channel, buffer, rpcMessage);
  }

  private void encodeMessage(Channel channel, ChannelBuffer buffer, ProtobufMessage.RpcMessage rpcMessage) throws IOException {
    int bodyLen = rpcMessage.getSerializedSize();
    int headerLen = CodedOutputStream.computeRawVarint32Size(bodyLen);
    buffer.ensureWritableBytes(headerLen + bodyLen);

    CodedOutputStream headerOut = CodedOutputStream.newInstance(new ChannelBufferOutputStream(buffer), headerLen);

    headerOut.writeRawVarint32(bodyLen);
    headerOut.flush();

    buffer.writeBytes(rpcMessage.toByteArray());
  }

  public Object decode(Channel channel, ChannelBuffer buffer) throws IOException {
    // 检查包头长度
    int readable = buffer.readableBytes();
    if (readable < HEADER_LENGTH) {
      return DecodeResult.NEED_MORE_INPUT;
    }
    // 检查数据包长度
    int msgLength = 0;
    byte[] buf = new byte[HEADER_LENGTH + 1];
    for (int i = 0; i < buf.length; ++i) {
      // 没算出长度时不可读则返回
      if (!(buffer.readable())) {
        return DecodeResult.NEED_MORE_INPUT;
      }
      // 查看可变长度是否符合要求
      buf[i] = buffer.readByte();
      if (buf[i] >= 0) {
        // 检查可变长度值
        int length = CodedInputStream.newInstance(buf, 0, i + 1).readRawVarint32();
        if (length < 0) {
          throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, "negative length: " + length);
        }
        // 判断剩余长度够不够
        if (buffer.readableBytes() < length) {
          return DecodeResult.NEED_MORE_INPUT;
        }
        // 找出可变长度
        msgLength = length;
        break;
      }
    }

    // 解析包体
    ChannelBufferInputStream is = new ChannelBufferInputStream(buffer, msgLength);
    try {
      return decodeMessage(channel, is);
    } finally {
      if (is.available() > 0) {
        try {
          if (logger.isWarnEnabled()) {
            logger.warn("Skip input stream " + is.available());
          }
          StreamUtils.skipUnusedStream(is);
        } catch (IOException e) {
          logger.warn(e.getMessage(), e);
        }
      }
    }

    // throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, "length wider than 32-bit");
  }

  private Object decodeMessage(Channel channel, InputStream is) throws IOException {

    ProtobufMessage.RpcMessage rpcMessage;
    try {
      rpcMessage = ProtobufMessage.RpcMessage.parseFrom(is);
    } catch (InvalidProtocolBufferException e) {
      throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e.getMessage(), e);
    }

    if (rpcMessage.getType() == PackageType.REQUEST || rpcMessage.getType() == PackageType.PUSH) {
      return decodeRequest(channel, rpcMessage);
    } else if (rpcMessage.getType() == PackageType.RESPONSE) {
      return decodeResponse(channel, rpcMessage);
    } else if (rpcMessage.getType() == PackageType.HEARTBEAT) {
      return decodeHeartbeat(channel, rpcMessage);
    }

    return null;
  }

  private Object decodeHeartbeat(Channel channel, ProtobufMessage.RpcMessage rpcMessage) {
    // 创建请求上下文
    Request request = new Request();
    request.setVersion("2.0.0");
    request.setTwoWay(true);
    request.setEvent(Request.HEARTBEAT_EVENT);

    return request;
  }

  private Object decodeRequest(Channel channel, ProtobufMessage.RpcMessage rpcMessage) {

    // 解析request包体
    ProtobufMessage.Request rpcRequest;
    try {
      rpcRequest = ProtobufMessage.Request.parseFrom(rpcMessage.getSerialized());
    } catch (InvalidProtocolBufferException e) {
      throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e.getMessage(), e);
    }

    // 获取请求信息
    long seqID = rpcMessage.getIdentify();
    String serviceName = rpcRequest.getService();
    String methodName = rpcRequest.getMethod();

    // 获取请求类型
    Class<?> requestClazz = ProtobufServices.getInstance().getRequestClass(serviceName, methodName);
    // 获取请求默认实例
    com.google.protobuf.Message requestProtoType = ProtobufServices.getInstance().getRequestType(serviceName, methodName);
    // 通过实例调用build创建请求
    com.google.protobuf.Message requestMessage;
    try {
      requestMessage = requestProtoType.newBuilderForType().mergeFrom(rpcRequest.getRequest()).build();
    } catch (InvalidProtocolBufferException e) {
      throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e.getMessage(), e);
    }

    List<Object> parameters = new ArrayList<Object>();
    List<Class<?>> parameterTypes = new ArrayList<Class<?>>();
    // 增加控制器
    parameterTypes.add(com.google.protobuf.RpcController.class);
    parameters.add(ProtobufController.getDefaultInstance());
    // 增加请求参数
    parameterTypes.add(requestClazz);
    parameters.add(requestMessage);

    // 创建请求会话
    RpcInvocation result = new RpcInvocation();
    result.setAttachment(Constants.INTERFACE_KEY, serviceName);
    result.setMethodName(methodName);
    result.setArguments(parameters.toArray());
    result.setParameterTypes(parameterTypes.toArray(new Class[parameterTypes.size()]));

    // 是否需要回报
    boolean bTwoWay = rpcMessage.getType() == ProtobufMessage.PackageType.REQUEST ? true : false;

    // 创建请求上下文
    Request request = new Request(seqID);
    request.setVersion("2.0.0");
    request.setTwoWay(bTwoWay);
    request.setData(result);

    return request;
  }

  private Object decodeResponse(Channel channel, ProtobufMessage.RpcMessage rpcMessage) {

    // 获取请求信息
    long seqID = rpcMessage.getIdentify();

    // 获取返回包对应的请求会话
    Invocation inv = (Invocation) getRequestInvocation(seqID);
    if (inv == null) {
      throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, "request invocation is null for response id" + seqID);
    }

    // 获取请求信息
    String serviceName = inv.getAttachment(Constants.INTERFACE_KEY);
    String methodName = inv.getMethodName();

    // 解析response包体
    ProtobufMessage.Response rpcResponse;
    try {
      rpcResponse = ProtobufMessage.Response.parseFrom(rpcMessage.getSerialized());
    } catch (InvalidProtocolBufferException e) {
      throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e.getMessage(), e);
    }

    // 获取请求类型
    // Class<?> responseClazz = ProtobufServices.getInstance().getResponseClass(serviceName,
    // methodName);
    // 获取请求默认实例
    com.google.protobuf.Message responseProtoType = ProtobufServices.getInstance().getResponseType(serviceName, methodName);
    // 通过实例调用build创建请求
    com.google.protobuf.Message responseMessage;
    try {
      responseMessage = responseProtoType.newBuilderForType().mergeFrom(rpcResponse.getResponse()).build();
    } catch (InvalidProtocolBufferException e) {
      throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e.getMessage(), e);
    }

    // 组建rpc回包
    RpcResult rpcResult = new RpcResult();
    if (rpcResponse.getResult() == ProtobufConstants.RESULT_OK) {
      rpcResult.setValue(responseMessage);
    } else {
      rpcResult.setException(new RpcException(RpcException.SERIALIZATION_EXCEPTION + rpcResponse.getResult()));
    }

    // 组建resonse回包
    Response response = new Response();
    response.setId(seqID);
    response.setResult(rpcResult);

    return response;
  }

  // 获取返回包对于的请求会话信息
  protected Object getRequestInvocation(long seqID) {
    DefaultFuture future = DefaultFuture.getFuture(seqID);
    if (future == null) {
      return null;
    }
    Request req = future.getRequest();
    if (req == null)
      return null;
    return req.getData();
  }

}
