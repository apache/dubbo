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
package org.apache.dubbo.qos.pu;

import org.apache.dubbo.remoting.api.ProtocolDetector;
import org.apache.dubbo.remoting.buffer.ChannelBuffer;

public class QosHTTP1Detector implements ProtocolDetector {
    private static boolean isHttp(int magic) {
        return magic == 'G' || magic == 'P';
    }

    @Override
    public Result detect(ChannelBuffer in) {
        if (in.readableBytes() < 2) {
            return Result.NEED_MORE_DATA;
        }
        final int magic = in.getByte(in.readerIndex());
        // h2 starts with "PR"
        if (isHttp(magic) && in.getByte(in.readerIndex()+1) != 'R' ){
            return Result.RECOGNIZED;
        }
        return Result.UNRECOGNIZED;
    }
}
