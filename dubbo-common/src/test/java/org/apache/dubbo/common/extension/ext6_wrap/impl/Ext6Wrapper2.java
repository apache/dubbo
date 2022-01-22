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
package org.apache.dubbo.common.extension.ext6_wrap.impl;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Wrapper;
import org.apache.dubbo.common.extension.ext6_wrap.WrappedExt;
import org.apache.dubbo.common.extension.ext6_wrap.WrappedExtWrapper;

import java.util.concurrent.atomic.AtomicInteger;

@Wrapper(mismatches = {"impl3", "impl4"})
public class Ext6Wrapper2 implements WrappedExt, WrappedExtWrapper {
    public static AtomicInteger echoCount = new AtomicInteger();
    WrappedExt origin;

    public Ext6Wrapper2(WrappedExt origin) {
        this.origin = origin;
    }

    public String echo(URL url, String s) {
        echoCount.incrementAndGet();
        return origin.echo(url, s);
    }

    public WrappedExt getOrigin() {
        return origin;
    }
}
