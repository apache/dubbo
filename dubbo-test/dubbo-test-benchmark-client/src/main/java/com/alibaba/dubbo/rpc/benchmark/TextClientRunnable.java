/**
 * Copyright 1999-2014 dangdang.com.
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
package com.alibaba.dubbo.rpc.benchmark;

import org.apache.commons.lang.StringUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

/**
 * @author lishen
 */
public class TextClientRunnable extends AbstractClientRunnable{

    private final Text text =  new Text(StringUtils.leftPad("", 50000));

    public TextClientRunnable(String protocol, String serialization, String targetIP, int targetPort, int clientNums, int rpcTimeout,
                              CyclicBarrier barrier, CountDownLatch latch, long startTime,
                              long endTime){
        super(protocol, serialization, targetIP, targetPort, clientNums, rpcTimeout, barrier, latch, startTime, endTime);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Object invoke(ServiceFactory serviceFactory) {
        EchoService echoService = (EchoService) serviceFactory.get(EchoService.class);
        return echoService.text(text);
    }
//
//    public static void main(String[] args) {
//        System.out.println( StringUtils.leftPad("", 1000).getBytes().length);
//    }
}
