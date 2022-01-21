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

package org.apache.dubbo.rpc.protocol.tri.call;

import org.apache.dubbo.remoting.api.Connection;
import org.apache.dubbo.rpc.protocol.tri.GrpcStatus;
import org.apache.dubbo.rpc.protocol.tri.pack.WrapResponseUnpack;
import org.apache.dubbo.rpc.protocol.tri.pack.GenericUnpack;
import org.apache.dubbo.triple.TripleWrapper;

public class WrapUnaryResponseCallListener extends UnaryCallListener {
    private final WrapResponseUnpack responseUnpack;

    public WrapUnaryResponseCallListener(long requestId, Connection connection, GenericUnpack genericUnpack) {
        super(requestId, connection);
        this.responseUnpack = new WrapResponseUnpack(genericUnpack);
    }

    @Override
    public void onMessage(Object message) {
        try {
            final Object unpack = responseUnpack.unpack((TripleWrapper.TripleResponseWrapper) message);
            super.onMessage(unpack);
        } catch (Throwable t) {
            final GrpcStatus status = GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                .withDescription("Deserialize response message failed")
                .withCause(t);
            onClose(status, null);
        }
    }
}
