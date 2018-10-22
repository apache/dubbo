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
package com.alibaba.dubbo.config.spring.listener;

import com.alibaba.dubbo.common.LockSwitch;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * 2018/10/16
 */
public class ContextRefreshedApplicationListener implements ApplicationListener<ApplicationEvent> {

    AtomicBoolean EXECUTED = new AtomicBoolean(false);

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        // spring init, will singleton. but need to be forbidden invoked twice.
        if(EXECUTED.get()){
            return;
        }
        if (event instanceof ContextRefreshedEvent) {
            EXECUTED.set(true);
            LockSwitch.INIT_TASK_COUNTER.decrementAndGet();
        }
    }
}
