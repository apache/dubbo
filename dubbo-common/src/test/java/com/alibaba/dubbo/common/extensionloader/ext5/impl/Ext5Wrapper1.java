/*
 * Copyright 1999-2011 Alibaba Group.
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
package com.alibaba.dubbo.common.extensionloader.ext5.impl;

import java.util.concurrent.atomic.AtomicInteger;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extensionloader.ext5.Ext5NoAdaptiveMethod;

/**
 * @author ding.lid
 */
public class Ext5Wrapper1 implements Ext5NoAdaptiveMethod {
    Ext5NoAdaptiveMethod instance;
    
    public static AtomicInteger echoCount = new AtomicInteger();
    public static AtomicInteger yellCount = new AtomicInteger();
    public static AtomicInteger bangCount = new AtomicInteger();
    
    public Ext5Wrapper1(Ext5NoAdaptiveMethod instance) {
        this.instance = instance;
    }
    
    public String echo(URL url, String s) {
        echoCount.incrementAndGet();
        return instance.echo(url, s);
    }

    public String yell(URL url, String s) {
        yellCount.incrementAndGet();
        return instance.yell(url, s);
    }

    public String bang(URL url, int i) {
        bangCount.incrementAndGet();
        return instance.bang(url, i);
    }
}