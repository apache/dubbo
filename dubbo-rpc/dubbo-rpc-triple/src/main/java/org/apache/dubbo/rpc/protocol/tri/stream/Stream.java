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

package org.apache.dubbo.rpc.protocol.tri.stream;


import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.rpc.protocol.tri.Compressor;
import org.apache.dubbo.rpc.protocol.tri.OutboundTransportObserver;
import org.apache.dubbo.rpc.protocol.tri.TransportObserver;

/**
 * Stream acts as a bi-directional intermediate layer for processing streaming data . It serializes object instance to
 * byte[] then send to remote, and deserializes byte[] to object instance from remote. {@link #inboundTransportObserver()}
 * and {@link #subscribe(OutboundTransportObserver)} provide {@link TransportObserver} to receive or send remote data.
 * {@link #inboundMessageObserver()} and {@link #subscribe(StreamObserver)} provide {@link StreamObserver}
 * as API for users fetching/emitting objects from/to remote peer.
 */
public interface Stream {

    URL url();

    long id();

    void setCompressor(Compressor compressor);

    Compressor compressor();

    Compressor decompressor();

    void setDecompressor(Compressor compressor);
}
