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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

/**
 * @author lishen
 */
public class BidClientRunnable extends AbstractClientRunnable{

    private final BidRequest request = new BidRequest();

    public BidClientRunnable(String protocol, String serialization, String targetIP, int targetPort, int clientNums, int rpcTimeout,
                              CyclicBarrier barrier, CountDownLatch latch, long startTime,
                              long endTime){
        super(protocol, serialization, targetIP, targetPort, clientNums, rpcTimeout, barrier, latch, startTime, endTime);
        Impression imp = new Impression();
        imp.setBidFloor(1.1);
        imp.setId("abc");
        List<Impression> imps = new ArrayList<Impression>(1);
        imps.add(imp);
        request.setImpressions(imps);

        Geo geo = new Geo();
        geo.setCity("beijing");
        geo.setCountry("china");
        geo.setLat(100.1f);
        geo.setLon(100.1f);

        Device device = new Device();
        device.setMake("apple");
        device.setOs("ios");
        device.setVersion("7.0");
        device.setLang("zh_CN");
        device.setModel("iphone");
        device.setGeo(geo);
        request.setDevice(device);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Object invoke(ServiceFactory serviceFactory) {
        EchoService echoService = (EchoService) serviceFactory.get(EchoService.class);
        BidRequest result = echoService.bid(request);
        return result;
    }
}
