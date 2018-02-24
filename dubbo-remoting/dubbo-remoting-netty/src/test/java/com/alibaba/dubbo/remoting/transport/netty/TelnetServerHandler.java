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
package com.alibaba.dubbo.remoting.transport.netty;

import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.exchange.ExchangeChannel;
import com.alibaba.dubbo.remoting.exchange.support.Replier;

/**
 * Date: 4/26/11
 * Time: 4:29 PM
 */
public class TelnetServerHandler implements Replier<String> {

    public Class<String> interest() {
        return String.class;
    }

    public Object reply(ExchangeChannel channel, String msg) throws RemotingException {
        // Generate and write a response.

        String response;
        if (msg.length() == 0) {
            response = "Please type something.\r\n";
        } else {
            response = "Did you say '" + msg + "'?\r\n";
        }
        //System.out.println(response);
        return response;
    }

}