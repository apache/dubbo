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
package org.apache.dubbo.config.stub;

import org.apache.dubbo.config.api.Box;
import org.apache.dubbo.config.api.DemoException;
import org.apache.dubbo.config.api.DemoService;
import org.apache.dubbo.config.api.User;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ConsumerDemoServiceLocal implements DemoService {

    private DemoService demoService;
    private static boolean isConnected = false;
    private static boolean isDisConnected = false;
    private static CountDownLatch isConnectedLatch = new CountDownLatch(1);
    private static CountDownLatch isDisConnectedLatch = new CountDownLatch(1);

    public ConsumerDemoServiceLocal(DemoService demoService) {
        this.demoService = demoService;
    }

    public void onConnectTest() {
        isConnected = true;
        isConnectedLatch.countDown();
    }

    public void onDisConnectTest() {
        isDisConnected = true;
        isDisConnectedLatch.countDown();
    }

    public static boolean isConnected() {
        try {
            isConnectedLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return isConnected;
    }

    public static boolean isDisConnected() {
        try {
            isDisConnectedLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return isDisConnected;
    }


    // ==========================================================================================
    // The following can be ignored
    // ==========================================================================================

    @Override
    public String sayName(String name) {
        return demoService.sayName(name);
    }

    @Override
    public Box getBox() {
        return null;
    }

    @Override
    public void throwDemoException() throws DemoException {

    }

    @Override
    public List<User> getUsers(List<User> users) {
        return null;
    }

    @Override
    public int echo(int i) {
        return 0;
    }
}
