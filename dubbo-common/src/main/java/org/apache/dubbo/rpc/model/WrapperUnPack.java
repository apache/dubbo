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

public interface WrapperUnPack extends UnPack {

    /**
     * Stream-based unpack InputStream with exception handling
     */
    @Override
    default Object unpack(InputStream input) throws IOException {
        return unpack(input, false);
    }

    /**
     * Stream-based unpack InputStream with exception handling option
     */
    Object unpack(InputStream input, boolean isReturnTriException) throws IOException;

    /**
     * @deprecated Use {@link #unpack(InputStream)} for stream-based processing
     */
    @Deprecated
    default Object unpack(byte[] data) throws IOException {
        return unpack(data, false);
    }

    /**
     * @deprecated Use {@link #unpack(InputStream, boolean)} for stream-based processing
     */
    @Deprecated
    default Object unpack(byte[] data, boolean isReturnTriException) throws IOException {
        return unpack(new ByteArrayInputStream(data), isReturnTriException);
    }
}
