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

/**
 * A state for recording stream
 * A normal state transition :
 * Meta -> (EndStream) -> Data -> (EndStream) -> (Rst)
 */
public class TransportState {

    private static final int META_SEND = 0b00000000000000000000000000000001;
    private static final int RESET_SEND = 0b00000000000000000000000000000010;
    private static final int END_STREAM_SEND = 0b00000000000000000000000000000100;
    private static final int SERVER_SEND_STREAM_RECEIVED = 0b00000000000000000000000000001000;
    private static final int ALLOW_META_SEND = 0b00000000000000000000000000000000;
    private static final int ALLOW_DATA_SEND = META_SEND;
    private volatile int state = 0;

    public void setMetaSend() {
        this.state = this.state | META_SEND;
    }

    public void setResetSend() {
        this.state = this.state | RESET_SEND;
    }

    public void setEndStreamSend() {
        this.state = this.state | END_STREAM_SEND;
    }

    public void setServerEndStreamReceived() {
        this.state = this.state | SERVER_SEND_STREAM_RECEIVED;
    }

    public boolean serverSendStreamReceived() {
        return (this.state & SERVER_SEND_STREAM_RECEIVED) > 0;
    }

    public boolean allowSendMeta() {
        return this.state == ALLOW_META_SEND;
    }

    public boolean allowSendReset() {
        return (this.state & RESET_SEND) != RESET_SEND;
    }

    public boolean allowSendData() {
        return this.state == ALLOW_DATA_SEND;
    }

    public boolean allowSendEndStream() {
        return allowSendReset() && (this.state & END_STREAM_SEND) != END_STREAM_SEND;
    }

}
