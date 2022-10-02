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

import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2WindowUpdateFrame;

public class TripleFlowControl {

    private Http2Connection http2Connection;

    private int windowSizeIncrement;

    private Http2WindowUpdateFrame http2WindowUpdateFrame;

    public TripleFlowControl(Http2Connection http2Connection,int windowSizeIncrement,Http2WindowUpdateFrame http2WindowUpdateFrame){
        this.http2Connection = http2Connection;
        this.windowSizeIncrement = windowSizeIncrement;
        this.http2WindowUpdateFrame = http2WindowUpdateFrame;
    }

    public Http2Connection getHttp2Connection() {
        return http2Connection;
    }

    public void setHttp2Connection(Http2Connection http2Connection) {
        this.http2Connection = http2Connection;
    }

    public int getWindowSizeIncrement() {
        return windowSizeIncrement;
    }

    public void setWindowSizeIncrement(int windowSizeIncrement) {
        this.windowSizeIncrement = windowSizeIncrement;
    }

    public Http2WindowUpdateFrame getHttp2WindowUpdateFrame() {
        return http2WindowUpdateFrame;
    }

    public void setHttp2WindowUpdateFrame(Http2WindowUpdateFrame http2WindowUpdateFrame) {
        this.http2WindowUpdateFrame = http2WindowUpdateFrame;
    }

}
