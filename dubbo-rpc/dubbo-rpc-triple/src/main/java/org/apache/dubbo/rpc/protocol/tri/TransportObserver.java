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

import io.netty.handler.codec.http2.Http2Error;

public interface TransportObserver {

    static int calcCompressFlag(Compressor compressor) {
        if (null == compressor || IdentityCompressor.NONE.equals(compressor)) {
            return 0;
        }
        return 1;
    }

    void onMetadata(Metadata metadata, boolean endStream);

    void onData(byte[] data, boolean endStream);

    default void onReset(Http2Error http2Error) {
    }

    default void onComplete() {
    }

}
