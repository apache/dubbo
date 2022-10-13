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

package org.apache.dubbo.rpc.protocol.tri.stream;

import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.protocol.tri.TripleFlowControlFrame;
import java.util.Map;

public class MockClientStreamListener implements ClientStream.Listener {

    public TriRpcStatus status;
    public byte[] message;
    public boolean started;

    @Override
    public void onStart() {
        started = true;
    }

    @Override
    public void onComplete(TriRpcStatus status,
        Map<String, Object> attachments) {
        this.status = status;
    }

    @Override
    public void onMessage(TripleFlowControlFrame message) {
        this.message = message.getMessage();
    }

    @Override
    public void onCancelByRemote(TriRpcStatus status) {

    }
}
