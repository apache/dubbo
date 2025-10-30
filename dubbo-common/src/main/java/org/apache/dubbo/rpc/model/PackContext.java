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
package org.apache.dubbo.rpc.model;

import java.io.OutputStream;

/**
 * Pack context for zero-copy optimization.
 * Encapsulates both size calculation and stream writing capability.
 */
public interface PackContext {

    /**
     * Get the packed size in bytes
     * @return size in bytes
     */
    int getSize();

    /**
     * Write packed data to output stream
     * @param os output stream
     * @throws Exception if write fails
     */
    void writeTo(OutputStream os) throws Exception;

    /**
     * Create a PackContext from byte array (fallback implementation)
     * @param bytes byte array
     * @return PackContext instance
     */
    static PackContext of(byte[] bytes) {
        return new ByteArrayPackContext(bytes);
    }

    /**
     * Default implementation backed by byte array
     */
    class ByteArrayPackContext implements PackContext {
        private final byte[] bytes;

        ByteArrayPackContext(byte[] bytes) {
            this.bytes = bytes;
        }

        @Override
        public int getSize() {
            return bytes.length;
        }

        @Override
        public void writeTo(OutputStream os) throws Exception {
            os.write(bytes);
        }
    }
}
