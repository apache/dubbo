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
package org.apache.dubbo.remoting.api;


import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.buffer.ChannelBuffer;


/**
 * Determine incoming bytes belong to the specific protocol.
 */
public interface ProtocolDetector {
    int empty = ' ';

    default Result detect(ChannelBuffer in) {
        return Result.UNRECOGNIZED;
    }

    default Result detect(ChannelBuffer in, URL url) {
        return detect(in);
    }


    enum Result {
        RECOGNIZED, UNRECOGNIZED, NEED_MORE_DATA
    }

    default int getByteByIndex(ChannelBuffer buffer, int index) {
        return buffer.getByte(buffer.readerIndex() + index);
    }

    default boolean prefixMatch(char[][] prefixes, ChannelBuffer buffer, int length) {
        // prefix match
        for (char[] prefix : prefixes) {

            for (int j = 0; j < length; j++) {
                if (prefix[j] != getByteByIndex(buffer, j)) {
                    break;
                }
                return true;
            }
        }
        return false;
    }

    /**
     * between first and second empty char
     *
     * @param buffer
     * @return
     */
    default String readRequestLine(ChannelBuffer buffer) {

        // GET /test/demo HTTP/1.1
        int firstEmptyIndex = 0;
        // read first empty
        for (int i = 0; i < Integer.MAX_VALUE; i++) {

            int read = getByteByIndex(buffer, i);
            if (read == empty) {
                firstEmptyIndex = i;
                break;
            }
        }

        StringBuilder stringBuilder = new StringBuilder();

        for (int i = firstEmptyIndex + 1; i < Integer.MAX_VALUE; i++) {
            int read = getByteByIndex(buffer, i);
            // second empty break
            if (read == empty) {
                break;
            }
            stringBuilder.append((char) read);
        }

        return stringBuilder.toString();

    }
}
