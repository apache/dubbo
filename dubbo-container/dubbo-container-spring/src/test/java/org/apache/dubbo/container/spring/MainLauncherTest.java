
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
package org.apache.dubbo.container.spring;

import java.util.concurrent.TimeUnit;

import org.apache.dubbo.container.Main;
import org.junit.Assert;
import org.junit.Test;


/**
 * SpringContainerTest
 *
 */
public class MainLauncherTest 
{
    @Test
    public void testMain() {
        System.setProperty(Main.SHUTDOWN_HOOK_KEY, "true");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e) {
                }
                Class<?> obj = SpringContainer.getContext().getBean("container").getClass();
                Assert.assertEquals(SpringContainer.class, obj);
                System.exit(0);
            }
        }).start();
        Main.main(null);
    }
}

