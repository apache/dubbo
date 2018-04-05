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

package com.alibaba.dubbo.rpc.protocol.dubbo;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.Codec2;
import com.alibaba.dubbo.remoting.buffer.ChannelBuffer;
import com.alibaba.dubbo.remoting.exchange.Request;
import com.alibaba.dubbo.remoting.exchange.Response;
import com.alibaba.dubbo.remoting.exchange.support.MultiMessage;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.RpcResult;

import java.io.IOException;

/**
 * 支持多消息的编解码器
 */
public final class DubboCountCodec implements Codec2 {

    /**
     * 编解码器
     */
    private DubboCodec codec = new DubboCodec();

    @Override
    public void encode(Channel channel, ChannelBuffer buffer, Object msg) throws IOException {
        codec.encode(channel, buffer, msg);
    }

    @Override
    public Object decode(Channel channel, ChannelBuffer buffer) throws IOException {
        // 记录当前读位置
        int save = buffer.readerIndex();
        // 创建 MultiMessage 对象
        MultiMessage result = MultiMessage.create();
        do {
            // 解码
            Object obj = codec.decode(channel, buffer);
            // 输入不够，重置读进度
            if (Codec2.DecodeResult.NEED_MORE_INPUT == obj) {
                buffer.readerIndex(save);
                break;
            // 解析到消息
            } else {
                // 添加结果消息
                result.addMessage(obj);
                // 记录消息长度到隐式参数集合，用于 MonitorFilter 监控
                logMessageLength(obj, buffer.readerIndex() - save);
                // 记录当前读位置
                save = buffer.readerIndex();
            }
        } while (true);
        // 需要更多的输入
        if (result.isEmpty()) {
            return Codec2.DecodeResult.NEED_MORE_INPUT;
        }
        // 返回解析到的消息
        if (result.size() == 1) {
            return result.get(0);
        }
        return result;
    }

    private void logMessageLength(Object result, int bytes) {
        if (bytes <= 0) {
            return;
        }
        if (result instanceof Request) {
            try {
                ((RpcInvocation) ((Request) result).getData()).setAttachment(Constants.INPUT_KEY, String.valueOf(bytes)); // 请求
            } catch (Throwable e) {
                /* ignore */
            }
        } else if (result instanceof Response) {
            try {
                ((RpcResult) ((Response) result).getResult()).setAttachment(Constants.OUTPUT_KEY, String.valueOf(bytes)); // 响应
            } catch (Throwable e) {
                /* ignore */
            }
        }
    }

}
