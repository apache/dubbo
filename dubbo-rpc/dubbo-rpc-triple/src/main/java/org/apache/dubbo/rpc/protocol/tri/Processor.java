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
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.triple.TripleWrapper;

public class Processor {
    private final AbstractStream stream;
    private final MethodDescriptor md;
    private final MultipleSerialization multipleSerialization;
    private final URL url;
    private String serializeType;

    public Processor(AbstractStream stream, MethodDescriptor md, URL url, String serializeType,
        MultipleSerialization multipleSerialization) {
        this.stream = stream;
        this.md = md;
        this.url = url;
        this.serializeType = serializeType;
        this.multipleSerialization = multipleSerialization;
    }

    public void onSingleMessage(InputStream in) {
        // todo executor
        final Object[] resp = decodeRequestMessage(in);
        if (resp.length > 1) {
            return;
        }
        stream.getObserver().onNext(resp[0]);

    }

    public Object[] decodeRequestMessage(InputStream is) {
        if (md.isNeedWrap()) {
            final TripleWrapper.TripleRequestWrapper req = TripleUtil.unpack(is,
                TripleWrapper.TripleRequestWrapper.class);
            this.serializeType = req.getSerializeType();
            String[] paramTypes = req.getArgTypesList().toArray(new String[req.getArgsCount()]);
            if (!Arrays.equals(this.md.getCompatibleParamSignatures(), paramTypes)) {
                throw new IllegalArgumentException("paramTypes is not ");
            }
            final Object[] arguments = TripleUtil.unwrapReq(url, req, multipleSerialization);
            return arguments;
        } else {
            final Object req = TripleUtil.unpack(is, md.getParameterClasses()[0]);
            return new Object[] {req};
        }
    }

    public Object decodeResponseMessage(InputStream is) {
        final Object resp;
        if (md.isNeedWrap()) {
            final TripleWrapper.TripleResponseWrapper message = TripleUtil.unpack(is,
                TripleWrapper.TripleResponseWrapper.class);
            resp = TripleUtil.unwrapResp(url, message, multipleSerialization);
        } else {
            resp = TripleUtil.unpack(is, md.getReturnClass());
        }
        return resp;
    }

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
