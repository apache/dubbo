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

package org.apache.dubbo.rpc;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPromise;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.rpc.model.ApplicationModel;

public class CancellableContextImpl implements CancellableContext {

    private static final Map<StreamObserver, Channel> CHANNEL_MAP = new ConcurrentHashMap<>();

    @Override
    public ChannelPromise cancel(StreamObserver<?> streamObserver) {
        Channel channel = CHANNEL_MAP.remove(streamObserver);
        CallStream callStream = ApplicationModel.defaultModel().getDefaultExtension(CallStream.class);
        return callStream.cancel(channel);
    }

    @Override
    public void release(StreamObserver<?> streamObserver) {
        CHANNEL_MAP.remove(streamObserver);
    }

    @Override
    public void registerChannel(StreamObserver<?> streamObserver, Channel channel) {
        CHANNEL_MAP.put(streamObserver, channel);
    }

}
