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

import org.apache.dubbo.rpc.model.PackableMethod;

import io.netty.buffer.ByteBuf;

public interface ByteBufPackableMethod extends PackableMethod {

    default Object parseRequest(ByteBuf buffer) throws Exception {
        return getRequestUnpack().unpack(buffer);
    }

    default Object parseResponse(ByteBuf data) throws Exception {
        return parseResponse(data, false);
    }

    default Object parseResponse(ByteBuf data, boolean isReturnTriException) throws Exception {
        ByteBufUnPack unPack = getResponseUnpack();
        if (unPack instanceof ByteBufWrapperUnPack) {
            return ((ByteBufWrapperUnPack) unPack).unpack(data, isReturnTriException);
        }
        return unPack.unpack(data);
    }

    default void packRequest(ByteBuf buffer, Object request) throws Exception {
        getRequestPack().pack(request);
    }

    default void packResponse(ByteBuf buffer, Object response) throws Exception {
        getResponsePack().pack(buffer, response);
    }

    ByteBufPack getRequestPack();

    ByteBufPack getResponsePack();

    ByteBufUnPack getResponseUnpack();

    ByteBufUnPack getRequestUnpack();
}
