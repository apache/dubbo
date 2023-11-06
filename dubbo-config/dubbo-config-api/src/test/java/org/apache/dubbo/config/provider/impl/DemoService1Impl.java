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
package org.apache.dubbo.config.provider.impl;

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.config.api.DemoService1;
import org.apache.dubbo.config.api.HelloReply;
import org.apache.dubbo.config.api.HelloRequest;

public class DemoService1Impl implements DemoService1 {
    @Override
    public HelloReply sayHello(HelloRequest request) {
        return null;
    }

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> streamObserver) {

    }
}
