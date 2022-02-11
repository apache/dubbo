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

import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.protocol.tri.DefaultFuture2;
import org.apache.dubbo.rpc.protocol.tri.RpcStatus;

import java.util.Map;

public class UnaryCallListener implements ClientCall.Listener {
    private final long requestId;
    private Object appResponse;
    private boolean closed;

    public UnaryCallListener(long requestId) {
        this.requestId = requestId;
    }

    @Override
    public void onMessage(Object message) {
        this.appResponse = message;
    }

    @Override
    public void onClose(RpcStatus status, Map<String, Object> trailers) {
        if (closed) {
            return;
        }
        closed = true;
        AppResponse result = new AppResponse();
        result.setObjectAttachments(trailers);
        if (status.isOk()) {
            result.setValue(appResponse);
        } else {
            result.setException(status.asException());
        }
        DefaultFuture2.received(requestId, status, result);
    }
}
