/*
 * Copyright 2006-2014 handu.com.
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
package com.alibaba.dubbo.demo.consumer;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.demo.bid.*;
import com.alibaba.dubbo.demo.user.User;
import com.alibaba.dubbo.demo.user.facade.AnotherUserRestService;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Jinkai.Ma
 */
@Component
public class DemoJavaConfigAction {

    @Reference
    private BidService bidService;

    @Reference
    private AnotherUserRestService anotherUserRestService;

    @PostConstruct
    public void start() throws Exception {
        BidRequest request = new BidRequest();

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

//        long start = System.currentTimeMillis();

//        for (int i = 0; i < 10000; i ++) {
//        System.out.println(bidService.bid(request).getId());
        System.out.println("SUCESS: got bid response id: " + bidService.bid(request).getId());
//        }

//        System.out.println(">>>>> Total time consumed:" + (System.currentTimeMillis() - start));

        try {
            bidService.throwNPE();
            System.out.println("ERROR: no exception found");
        } catch (NullPointerException e) {
            System.out.println("SUCCESS: caught exception " + e.getClass());
        }

        User user = new User(1L, "larrypage");
        System.out.println("SUCESS: registered user with id " + anotherUserRestService.registerUser(user).getId());

        System.out.println("SUCESS: got user " + anotherUserRestService.getUser(1L));
    }
}
