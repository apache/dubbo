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
package com.alibaba.dubbo.remoting;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Adaptive;
import com.alibaba.dubbo.common.extension.SPI;
import com.alibaba.dubbo.remoting.buffer.ChannelBuffer;

import java.io.IOException;

/**
 * 编解码器接口
 */
@SPI
public interface Codec2 {

    /**
     * 编码
     *
     * @param channel 通道
     * @param buffer Buffer
     * @param message 消息
     * @throws IOException 当编码发生异常时
     */
    @Adaptive({Constants.CODEC_KEY})
    void encode(Channel channel, ChannelBuffer buffer, Object message) throws IOException;

    /**
     * 解码
     *
     * @param channel 通道
     * @param buffer Buffer
     * @return 消息
     * @throws IOException 当解码发生异常时
     */
    @Adaptive({Constants.CODEC_KEY})
    Object decode(Channel channel, ChannelBuffer buffer) throws IOException;

    /**
     * 解码结果
     */
    enum DecodeResult {
        /**
         * 需要更多输入
         */
        NEED_MORE_INPUT,
        /**
         * 忽略一些输入
         */
        SKIP_SOME_INPUT
    }

}

