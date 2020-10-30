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
import org.apache.dubbo.remoting.Codec2;
import org.apache.dubbo.remoting.buffer.ChannelBuffer;
import org.apache.dubbo.remoting.buffer.ChannelBufferOutputStream;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.rpc.Invocation;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TMessageType;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TStruct;
import org.apache.thrift.transport.TIOStreamTransport;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
/**
 * @since 2.7.0, use https://github.com/dubbo/dubbo-rpc-native-thrift instead
 */
@Deprecated
public class ThriftNativeCodec implements Codec2 {

    private final AtomicInteger thriftSeq = new AtomicInteger(0);

    protected static TProtocol newProtocol(URL url, ChannelBuffer buffer) throws IOException {
        String protocol = url.getParameter(ThriftConstants.THRIFT_PROTOCOL_KEY,
                ThriftConstants.DEFAULT_PROTOCOL);
        if (ThriftConstants.BINARY_THRIFT_PROTOCOL.equals(protocol)) {
            return new TBinaryProtocol(new TIOStreamTransport(new ChannelBufferOutputStream(buffer)));
        }
        throw new IOException("Unsupported protocol type " + protocol);
    }

    @Override
    public void encode(Channel channel, ChannelBuffer buffer, Object message)
            throws IOException {
        if (message instanceof Request) {
            encodeRequest(channel, buffer, (Request) message);
        } else if (message instanceof Response) {
            encodeResponse(channel, buffer, (Response) message);
        } else {
            throw new IOException("Unsupported message type "
                    + message.getClass().getName());
        }
    }

    protected void encodeRequest(Channel channel, ChannelBuffer buffer, Request request)
            throws IOException {
        Invocation invocation = (Invocation) request.getData();
        TProtocol protocol = newProtocol(channel.getUrl(), buffer);
        try {
            protocol.writeMessageBegin(new TMessage(
                    invocation.getMethodName(), TMessageType.CALL,
                    thriftSeq.getAndIncrement()));
            protocol.writeStructBegin(new TStruct(invocation.getMethodName() + "_args"));
            for (int i = 0; i < invocation.getParameterTypes().length; i++) {
                Class<?> type = invocation.getParameterTypes()[i];

            }
        } catch (TException e) {
            throw new IOException(e.getMessage(), e);
        }

    }

    protected void encodeResponse(Channel channel, ChannelBuffer buffer, Response response)
            throws IOException {

    }

    @Override
    public Object decode(Channel channel, ChannelBuffer buffer) throws IOException {
        return null;
    }

}
