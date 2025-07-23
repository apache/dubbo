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

import org.apache.dubbo.remoting.buffer.ChannelBuffer;

import java.util.HashMap;
import java.util.Map;

/**
 * Determine incoming bytes belong to the specific protocol.
 */
public interface ProtocolDetector {

    Result detect(ChannelBuffer in);

    class Result {

        private final Flag flag;

        private final Map<String, String> detectContext = new HashMap<>(4);

        private Result(Flag flag) {
            this.flag = flag;
        }

        public void setAttribute(String key, String value) {
            this.detectContext.put(key, value);
        }

        public String getAttribute(String key) {
            return this.detectContext.get(key);
        }

        public void removeAttribute(String key) {
            this.detectContext.remove(key);
        }

        public Flag flag() {
            return flag;
        }

        public static Result recognized() {
            return new Result(Flag.RECOGNIZED);
        }

        public static Result unrecognized() {
            return new Result(Flag.UNRECOGNIZED);
        }

        public static Result needMoreData() {
            return new Result(Flag.NEED_MORE_DATA);
        }

        @Override
        public int hashCode() {
            return flag.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Result && flag == ((Result) obj).flag;
        }
    }

    enum Flag {
        RECOGNIZED,
        UNRECOGNIZED,
        NEED_MORE_DATA
    }

    default int getByteByIndex(ChannelBuffer buffer, int index) {
        return buffer.getByte(buffer.readerIndex() + index);
    }

    default boolean prefixMatch(char[][] prefixes, ChannelBuffer buffer, int length) {

        int[] ints = new int[length];
        for (int i = 0; i < length; i++) {
            ints[i] = getByteByIndex(buffer, i);
        }

        // prefix match
        for (char[] prefix : prefixes) {

            boolean matched = true;
            for (int j = 0; j < length; j++) {
                if (prefix[j] != ints[j]) {
                    matched = false;
                    break;
                }
            }

            if (matched) {
                return true;
            }
        }
        return false;
    }
}
