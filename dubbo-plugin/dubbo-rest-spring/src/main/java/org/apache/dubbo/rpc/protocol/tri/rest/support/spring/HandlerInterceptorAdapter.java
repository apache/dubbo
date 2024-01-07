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
package org.apache.dubbo.rpc.protocol.tri.rest.support.spring;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.protocol.tri.rest.filter.RestExtensionAdapter;
import org.apache.dubbo.rpc.protocol.tri.rest.filter.RestFilter;

import org.springframework.web.servlet.HandlerInterceptor;

@Activate(onClass = "org.springframework.web.servlet.HandlerInterceptor")
public class HandlerInterceptorAdapter implements RestExtensionAdapter<HandlerInterceptor> {
    @Override
    public boolean accept(Object extension) {
        return false;
    }

    @Override
    public RestFilter adapt(HandlerInterceptor extension) {
        return null;
    }
}
