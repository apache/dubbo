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
package com.alibaba.dubbo.rpc.benchmark;

import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.exchange.ExchangeChannel;
import com.alibaba.dubbo.remoting.exchange.Exchangers;
import com.alibaba.dubbo.remoting.exchange.support.ExchangeHandlerAdapter;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Abstract benchmark server Usage: BenchmarkServer listenPort maxThreads responseSize
 */
public abstract class AbstractBenchmarkServer {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public void run(String[] args) throws Exception {
        if (args == null || args.length != 5) {
            throw new IllegalArgumentException(
                    "must give three args: listenPort | maxThreads | responseSize | transporter | serialization");
        }
        int listenPort = Integer.parseInt(args[0]);
        int maxThreads = Integer.parseInt(args[1]);
        final int responseSize = Integer.parseInt(args[2]);
        String transporter = args[3];
        String serialization = args[4];
        System.out.println(dateFormat.format(new Date()) + " ready to start server,listenPort is: " + listenPort
                + ",maxThreads is:" + maxThreads + ",responseSize is:" + responseSize
                + " bytes,transporter is:" + transporter + ",serialization is:" + serialization);
        StringBuilder url = new StringBuilder();
        url.append("exchange://0.0.0.0:");
        url.append(listenPort);
        url.append("?transporter=");
        url.append(transporter);
        url.append("&serialization=");
        url.append(serialization);
        url.append("&threads=");
        url.append(maxThreads);
        Exchangers.bind(url.toString(), new ExchangeHandlerAdapter() {

            public Object reply(ExchangeChannel channel, Object message) throws RemotingException {
                return new ResponseObject(responseSize); // send response
            }
        });
    }
}
