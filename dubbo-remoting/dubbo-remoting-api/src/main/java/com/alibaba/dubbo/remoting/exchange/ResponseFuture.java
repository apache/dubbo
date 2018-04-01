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

import com.alibaba.dubbo.remoting.RemotingException;

/**
 * Future. (API/SPI, Prototype, ThreadSafe)
 *
 * 响应 Future
 *
 * @see com.alibaba.dubbo.remoting.exchange.ExchangeChannel#request(Object)
 * @see com.alibaba.dubbo.remoting.exchange.ExchangeChannel#request(Object, int)
 */
public interface ResponseFuture {

    /**
     * get result.
     * 获得值
     *
     * @return result.
     */
    Object get() throws RemotingException;

    /**
     * get result with the specified timeout.
     * 获得值
     *
     * @param timeoutInMillis timeout. 超时时长
     * @return result.
     */
    Object get(int timeoutInMillis) throws RemotingException;

    /**
     * set callback.
     * 设置回调
     *
     * @param callback 回调
     */
    void setCallback(ResponseCallback callback);

    /**
     * check is done.
     * 是否完成
     *
     * @return done or not.
     */
    boolean isDone();

}