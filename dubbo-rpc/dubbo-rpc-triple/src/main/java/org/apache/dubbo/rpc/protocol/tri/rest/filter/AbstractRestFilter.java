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
package org.apache.dubbo.rpc.protocol.tri.rest.filter;

import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RestUtils;

import java.util.Arrays;

public abstract class AbstractRestFilter<E> implements RestFilter {

    protected final E extension;

    public AbstractRestFilter(E extension) {
        this.extension = extension;
    }

    @Override
    public int getPriority() {
        return RestUtils.getPriority(extension);
    }

    @Override
    public String[] getPatterns() {
        return RestUtils.getPattens(extension);
    }

    @Override
    public void doFilter(HttpRequest request, HttpResponse response, FilterChain chain) throws Exception {
        chain.doFilter(request, response);
    }

    public void onSuccess(Result result, HttpRequest request, HttpResponse response) throws Exception {}

    public void onError(Throwable t, HttpRequest request, HttpResponse response) throws Exception {}

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("RestFilter{extension=");
        sb.append(extension);
        int priority = getPriority();
        if (priority != 0) {
            sb.append(", priority=").append(priority);
        }
        String[] patterns = getPatterns();
        if (patterns != null) {
            sb.append(", patterns=").append(Arrays.toString(patterns));
        }
        return sb.append('}').toString();
    }
}
