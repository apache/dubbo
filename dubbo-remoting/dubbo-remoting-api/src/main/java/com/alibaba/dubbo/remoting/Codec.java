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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Codec. (SPI, Singleton, ThreadSafe)
 */
@Deprecated
@SPI
public interface Codec {

    /**
     * Need more input poison.
     *
     * @see #decode(Channel, InputStream)
     */
    Object NEED_MORE_INPUT = new Object();

    /**
     * Encode message.
     *
     * @param channel channel.
     * @param output  output stream.
     * @param message message.
     */
    @Adaptive({Constants.CODEC_KEY})
    void encode(Channel channel, OutputStream output, Object message) throws IOException;

    /**
     * Decode message.
     *
     * @param channel channel.
     * @param input   input stream.
     * @return message or <code>NEED_MORE_INPUT</code> poison.
     * @see #NEED_MORE_INPUT
     */
    @Adaptive({Constants.CODEC_KEY})
    Object decode(Channel channel, InputStream input) throws IOException;

}