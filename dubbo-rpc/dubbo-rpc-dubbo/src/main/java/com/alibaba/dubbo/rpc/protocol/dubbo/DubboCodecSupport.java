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
package com.alibaba.dubbo.rpc.protocol.dubbo;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.serialize.Serialization;
import com.alibaba.dubbo.remoting.transport.CodecSupport;
import com.alibaba.dubbo.rpc.Invocation;

import static com.alibaba.dubbo.common.Constants.SERIALIZATION_ID_KEY;

public class DubboCodecSupport {
    public static Serialization getRequestSerialization(URL url, Invocation invocation) {
        Object serializationType_obj = invocation.get(SERIALIZATION_ID_KEY);
        if (serializationType_obj != null) {
            return CodecSupport.getSerializationById((Byte) serializationType_obj);
        }
        return ExtensionLoader.getExtensionLoader(Serialization.class).getExtension(
                url.getParameter(Constants.SERIALIZATION_KEY, Constants.DEFAULT_REMOTING_SERIALIZATION));
    }

    public static Serialization getResponseSerialization(URL url, DecodeableRpcResult result) {
        Invocation invocation = result.getInvocation();
        if (invocation != null) {
            Object serializationType_obj = invocation.get(SERIALIZATION_ID_KEY);
            if (serializationType_obj != null) {
                return CodecSupport.getSerializationById((Byte) serializationType_obj);
            }
        }
        return ExtensionLoader.getExtensionLoader(Serialization.class).getExtension(
                url.getParameter(Constants.SERIALIZATION_KEY, Constants.DEFAULT_REMOTING_SERIALIZATION));
    }

}
