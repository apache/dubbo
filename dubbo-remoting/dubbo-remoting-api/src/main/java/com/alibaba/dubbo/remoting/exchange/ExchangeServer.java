/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.remoting.exchange;

import com.alibaba.dubbo.remoting.Server;

import java.net.InetSocketAddress;
import java.util.Collection;

/**
 * ExchangeServer. (API/SPI, Prototype, ThreadSafe)
 *
 * @author william.liangf
 */
public interface ExchangeServer extends Server {

    /**
     * get channels.
     *
     * @return channels
     */
    Collection<ExchangeChannel> getExchangeChannels();

    /**
     * get channel.
     *
     * @param remoteAddress
     * @return channel
     */
    ExchangeChannel getExchangeChannel(InetSocketAddress remoteAddress);

}