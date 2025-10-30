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

public interface Pack {

    /**
     * Pack object to byte array
     * @param obj instance
     * @return byte array
     * @throws Exception when error occurs
     */
    byte[] pack(Object obj) throws Exception;

    /**
     * Check if this Pack implementation supports zero-copy stream packing
     * @return true if supports stream packing
     */
    default boolean supportsStreamPacking() {
        return false;
    }

    /**
     * Create a PackContext for zero-copy optimization.
     * The context encapsulates both size calculation and stream writing.
     *
     * @param obj instance to pack
     * @return PackContext instance
     * @throws Exception when error occurs
     */
    default PackContext createPackContext(Object obj) throws Exception {
        byte[] bytes = pack(obj);
        return PackContext.of(bytes);
    }
}
