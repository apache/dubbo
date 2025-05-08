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
package org.apache.dubbo.remoting.http12;

import org.apache.dubbo.remoting.http12.exception.HttpOverPayloadException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class LimitedByteArrayOutputStream extends ByteArrayOutputStream {

    private final int capacity;

    public LimitedByteArrayOutputStream(int capacity) {
        super();
        this.capacity = capacity == 0 ? Integer.MAX_VALUE : capacity;
    }

    public LimitedByteArrayOutputStream(int size, int capacity) {
        super(size);
        this.capacity = capacity == 0 ? Integer.MAX_VALUE : capacity;
    }

    @Override
    public void write(int b) {
        ensureCapacity(1);
        super.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        ensureCapacity(b.length);
        super.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        ensureCapacity(len);
        super.write(b, off, len);
    }

    private void ensureCapacity(int len) {
        if (size() + len > capacity) {
            throw new HttpOverPayloadException("Response Entity Too Large");
        }
    }
}
