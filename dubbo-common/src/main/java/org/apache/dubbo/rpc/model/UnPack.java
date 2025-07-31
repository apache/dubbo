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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Zero-copy unpack interface using pure Java Stream API
 */
public interface UnPack {

    /**
     * Stream-based unpack InputStream to object for zero-copy processing
     * @param input InputStream containing packed data
     * @return object instance
     * @throws IOException when I/O error occurs
     */
    Object unpack(InputStream input) throws IOException;

    /**
     * @deprecated Use {@link #unpack(InputStream)} for stream-based processing
     */
    @Deprecated
    default Object unpack(byte[] data) throws IOException {
        return unpack(new ByteArrayInputStream(data));
    }
}
