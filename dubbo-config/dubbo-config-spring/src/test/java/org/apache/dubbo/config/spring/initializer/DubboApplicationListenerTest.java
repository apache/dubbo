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
package org.apache.dubbo.config.spring.initializer;

import org.apache.dubbo.config.DubboShutdownHook;
import org.apache.dubbo.bootstrap.DubboBootstrap;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class DubboApplicationListenerTest {

    @Test
    public void testTwoShutdownHook() {
        DubboShutdownHook spyHook = Mockito.spy(DubboShutdownHook.getDubboShutdownHook());
        ClassPathXmlApplicationContext applicationContext = getApplicationContext(spyHook, true);
        applicationContext.refresh();
        applicationContext.close();
        // shutdown hook can't be verified, because it will executed after main thread has finished.
        // so we can only verify it by manually run it.
        try {
            spyHook.start();
            spyHook.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Mockito.verify(spyHook, Mockito.times(2)).destroyAll();
    }

    @Test
    public void testOneShutdownHook() {
        DubboShutdownHook spyHook = Mockito.spy(DubboShutdownHook.getDubboShutdownHook());
        ClassPathXmlApplicationContext applicationContext = getApplicationContext(spyHook, false);
        applicationContext.refresh();
        applicationContext.close();
        Mockito.verify(spyHook, Mockito.times(1)).destroyAll();
    }

    private ClassPathXmlApplicationContext getApplicationContext(DubboShutdownHook hook, boolean registerHook) {
        DubboBootstrap bootstrap = new DubboBootstrap(registerHook, hook);
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext();
        applicationContext.addApplicationListener(new DubboApplicationListener(bootstrap));
        return applicationContext;
    }
}
