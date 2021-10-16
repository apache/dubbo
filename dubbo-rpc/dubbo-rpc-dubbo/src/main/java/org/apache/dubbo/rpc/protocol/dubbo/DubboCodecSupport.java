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
package org.apache.dubbo.rpc.protocol.dubbo;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.serialize.Serialization;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.transport.CodecSupport;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Invocation;

import static org.apache.dubbo.rpc.Constants.INVOCATION_KEY;
import static org.apache.dubbo.rpc.Constants.SERIALIZATION_ID_KEY;

public class DubboCodecSupport {

    public static Serialization getRequestSerialization(URL url, Invocation invocation) {
        Object serializationTypeObj = invocation.get(SERIALIZATION_ID_KEY);
        if (serializationTypeObj != null) {
            return CodecSupport.getSerializationById((byte) serializationTypeObj);
        }
        return url.getOrDefaultFrameworkModel().getExtensionLoader(Serialization.class).getExtension(
                url.getParameter(org.apache.dubbo.remoting.Constants.SERIALIZATION_KEY, Constants.DEFAULT_REMOTING_SERIALIZATION));
    }

    public static Serialization getResponseSerialization(URL url, AppResponse appResponse) {
        Object invocationObj = appResponse.getAttribute(INVOCATION_KEY);
        if (invocationObj != null) {
            Invocation invocation = (Invocation) invocationObj;
            Object serializationTypeObj = invocation.get(SERIALIZATION_ID_KEY);
            if (serializationTypeObj != null) {
                return CodecSupport.getSerializationById((byte) serializationTypeObj);
            }
        }
        return url.getOrDefaultFrameworkModel().getExtensionLoader(Serialization.class).getExtension(
                url.getParameter(Constants.SERIALIZATION_KEY, Constants.DEFAULT_REMOTING_SERIALIZATION));
    }
}
