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

package org.apache.dubbo.rpc.protocol.tri;

import java.io.InputStream;
import java.util.Arrays;

import com.google.protobuf.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.serialize.MultipleSerialization;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.triple.TripleWrapper;

public class Processor<T,R> {
    private final StreamObserver<T> subscriber;
    private final AbstractStream stream;
    private String serializeType;
    private final Class<T> inboundClass;
    private final Class<R> outboundClass;
    // inbound observer / handler
    // outbound observer /handler
    private final StreamObserver<?> inbound;
    private final StreamObserver<?> outbound;


    private StreamObserver<T> getSubscriber(){
        return subscriber;
    }
    public Processor(AbstractStream stream, Class<T> inbound, Class<R> outbound, StreamObserver<T> subscriber) {
        this.stream = stream;
        this.inboundClass=inbound;
        this.outboundClass=outbound;
        this.subscriber = subscriber;
    }

    public void onSingleMessage(InputStream in) {
        final T message= decodeMessage(in,inboundClass);
        getSubscriber().onNext(message);
    }
    public void sendSingleMessage()

    public static <M> M decodeMessage(InputStream is,Class<M> clz) {
        return TripleUtil.unpack(is, clz);
    }


//    public Object decodeResponseMessage(InputStream is) {
//        final Object resp;
//        if (md.isNeedWrap()) {
//            final TripleWrapper.TripleResponseWrapper message = TripleUtil.unpack(is,
//                TripleWrapper.TripleResponseWrapper.class);
//            resp = TripleUtil.unwrapResp(url, message, multipleSerialization);
//        } else {
//            resp = TripleUtil.unpack(is, md.getReturnClass());
//        }
//        return resp;
//    }

    public ByteBuf encodeResponse(Object value, ChannelHandlerContext ctx) {
        final Message message;
        final ByteBuf buf;

        if (md.isNeedWrap()) {
            message = TripleUtil.wrapResp(url, serializeType, value, md, multipleSerialization);
        } else {
            message = (Message)value;
        }
        buf = TripleUtil.pack(ctx, message);
        return buf;
    }

    public ByteBuf encodeRequest(RpcInvocation invocation, ChannelHandlerContext ctx) {
        outbound.onNext();
        final ByteBuf out;

        if (md.isNeedWrap()) {
            final TripleWrapper.TripleRequestWrapper wrap = TripleUtil.wrapReq(url, invocation, multipleSerialization);
            out = TripleUtil.pack(ctx, wrap);
        } else {
            out = TripleUtil.pack(ctx, invocation.getArguments()[0]);
        }
        return out;
    }
}
