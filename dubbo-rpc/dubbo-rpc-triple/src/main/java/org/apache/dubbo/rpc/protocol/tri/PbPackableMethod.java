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

import com.google.protobuf.Message;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.Pack;
import org.apache.dubbo.rpc.model.PackableMethod;
import org.apache.dubbo.rpc.model.UnPack;

import java.lang.reflect.ParameterizedType;

import static org.apache.dubbo.rpc.protocol.tri.TripleProtocol.METHOD_ATTR_PACK;

public class PbPackableMethod implements PackableMethod {

    private static final Pack PB_PACK = o -> ((Message) o).toByteArray();

    private final Pack requestPack;
    private final Pack responsePack;
    private final UnPack requestUnpack;
    private final UnPack responseUnpack;

    public PbPackableMethod(MethodDescriptor method) {
        Class<?>[] actualRequestTypes;
        Class<?> actualResponseType;
        switch (method.getRpcType()) {
            case CLIENT_STREAM:
            case BI_STREAM:
                actualRequestTypes = new Class<?>[]{
                    (Class<?>) ((ParameterizedType) method.getMethod()
                        .getGenericReturnType()).getActualTypeArguments()[0]};
                actualResponseType = (Class<?>) ((ParameterizedType) method.getMethod()
                    .getGenericParameterTypes()[0]).getActualTypeArguments()[0];
                break;
            case SERVER_STREAM:
                actualRequestTypes = method.getMethod().getParameterTypes();
                actualResponseType = (Class<?>) ((ParameterizedType) method.getMethod()
                    .getGenericParameterTypes()[1]).getActualTypeArguments()[0];
                break;
            case UNARY:
                actualRequestTypes = method.getParameterClasses();
                actualResponseType = (Class<?>) method.getReturnTypes()[0];
                break;
            default:
                throw new IllegalStateException("Can not reach here");
        }
        boolean singleArgument = method.getRpcType() != MethodDescriptor.RpcType.UNARY;
        this.requestPack = new PbArrayPacker(singleArgument);
        this.responsePack = PB_PACK;
        this.requestUnpack = new PbUnpack<>(actualRequestTypes[0]);
        this.responseUnpack = new PbUnpack<>(actualResponseType);
    }

    public static PbPackableMethod init(MethodDescriptor methodDescriptor, URL url) {
        Object stored = methodDescriptor.getAttribute(METHOD_ATTR_PACK);
        if (stored != null) {
            return (PbPackableMethod) stored;
        }
        PbPackableMethod pbPackableMethod = new PbPackableMethod(methodDescriptor);
        methodDescriptor.addAttribute(METHOD_ATTR_PACK, pbPackableMethod);
        return pbPackableMethod;
    }

    @Override
    public Pack getRequestPack() {
        return requestPack;
    }

    @Override
    public Pack getResponsePack() {
        return responsePack;
    }

    @Override
    public UnPack getResponseUnpack() {
        return responseUnpack;
    }

    @Override
    public UnPack getRequestUnpack() {
        return requestUnpack;
    }
}
