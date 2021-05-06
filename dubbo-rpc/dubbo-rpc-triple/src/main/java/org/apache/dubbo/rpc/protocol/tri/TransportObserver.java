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

public interface TransportObserver {
    Stream.OperationHandler EMPTY_HANDLER = (result, cause) -> {
    };

    default void tryOnMetadata(Metadata metadata, boolean endStream) {
        onMetadata(metadata, endStream, EMPTY_HANDLER);
    }

    default void tryOnData(byte[] data, boolean endStream) {
        onData(data, endStream, EMPTY_HANDLER);
    }

    default void tryOnComplete() {
        onComplete(EMPTY_HANDLER);
    }

    void onMetadata(Metadata metadata, boolean endStream, Stream.OperationHandler handler);

    void onData(byte[] data, boolean endStream, Stream.OperationHandler handler);

    void onComplete(Stream.OperationHandler handler);

}
