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
package com.alibaba.dubbo.remoting.exchange;

import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.RemotingException;

/**
 * ExchangeChannel. (API/SPI, Prototype, ThreadSafe)
 *
 * 信息交换通道接口
 */
public interface ExchangeChannel extends Channel {

    /**
     * send request.
     *
     * 发送请求
     *
     * @param request 请求
     * @return response future
     * @throws RemotingException 远程调用，发生异常
     */
    ResponseFuture request(Object request) throws RemotingException;

    /**
     * send request.
     *
     * 发送请求
     *
     * @param request 请求
     * @param timeout 超时时长
     * @return response future
     * @throws RemotingException 远程调用，发生异常
     */
    ResponseFuture request(Object request, int timeout) throws RemotingException;

    /**
     * get message handler.
     *
     * 获得信息交换处理器
     *
     * @return message handler
     */
    ExchangeHandler getExchangeHandler();

    /**
     * graceful close.
     *
     * 优雅关闭
     *
     * @param timeout 超时时长
     */
    void close(int timeout);

}