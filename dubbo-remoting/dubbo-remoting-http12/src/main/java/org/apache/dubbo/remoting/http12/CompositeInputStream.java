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

import org.apache.dubbo.remoting.http12.exception.DecodeException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CompositeInputStream extends InputStream {

    private final Queue<InputStream> inputStreams = new ConcurrentLinkedQueue<>();

    private int totalAvailable = 0;

    private int readIndex = 0;

    public void addInputStream(InputStream inputStream) {
        this.inputStreams.offer(inputStream);
        try {
            this.totalAvailable += inputStream.available();
        } catch (IOException e) {
            throw new DecodeException(e);
        }
    }

    @Override
    public int read() throws IOException {
        InputStream inputStream;
        while ((inputStream = inputStreams.peek()) != null) {
            int available = inputStream.available();
            if (available == 0) {
                releaseHeadStream();
                continue;
            }
            int read = inputStream.read();
            if (read != -1) {
                ++readIndex;
                releaseIfNecessary(inputStream);
                return read;
            }
            releaseHeadStream();
        }
        return -1;
    }

    @Override
    public int available() {
        return totalAvailable - readIndex;
    }

    @Override
    public void close() throws IOException {
        InputStream inputStream;
        while ((inputStream = inputStreams.poll()) != null) {
            inputStream.close();
        }
    }

    private void releaseHeadStream() throws IOException {
        InputStream removeStream = inputStreams.remove();
        removeStream.close();
    }

    private void releaseIfNecessary(InputStream inputStream) throws IOException {
        int available = inputStream.available();
        if (available == 0) {
            inputStream.close();
            releaseHeadStream();
        }
    }
}
