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
package org.apache.dubbo.rpc.cluster.router.tag;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.router.AbstractRouter;

import java.util.ArrayList;
import java.util.List;

/**
 * TagRouter
 */
public class TagRouter extends AbstractRouter {

    private static final Logger logger = LoggerFactory.getLogger(TagRouter.class);

    public static final URL ROUTER_URL = new URL("tag", Constants.ANYHOST_VALUE, 0, Constants.ANY_VALUE).addParameters(Constants.RUNTIME_KEY, "true");

    public TagRouter(URL url) {
        super(url.getParameter(Constants.PRIORITY_KEY, 0), url);
    }

    public TagRouter() {
        super(ROUTER_URL.getParameter(Constants.PRIORITY_KEY, 0), ROUTER_URL);
    }

    @Override
    public <T> List<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        // filter
        List<Invoker<T>> result = new ArrayList<>();
        try {
            // Dynamic param
            String tag = RpcContext.getContext().getAttachment(Constants.REQUEST_TAG_KEY);
            // Tag request
            if (!StringUtils.isEmpty(tag)) {
                // Select tag invokers first
                for (Invoker<T> invoker : invokers) {
                    if (tag.equals(invoker.getUrl().getParameter(Constants.TAG_KEY))) {
                        result.add(invoker);
                    }
                }
                // If no invoker be selected, downgrade to normal invokers
                if (result.isEmpty()) {
                    for (Invoker<T> invoker : invokers) {
                        if (StringUtils.isEmpty(invoker.getUrl().getParameter(Constants.TAG_KEY))) {
                            result.add(invoker);
                        }
                    }
                }
                // Normal request
            } else {
                for (Invoker<T> invoker : invokers) {
                    // Can't access tag invoker,only normal invoker should be selected
                    if (StringUtils.isEmpty(invoker.getUrl().getParameter(Constants.TAG_KEY))) {
                        result.add(invoker);
                    }
                }
            }
            return result;
        } catch (Exception e) {
            logger.error("Route by tag error,return all invokers.", e);
        }
        // Downgrade to all invokers
        return invokers;
    }

    @Override
    public Integer getPriority() {
        return priority;
    }

    @Override
    public int compareTo(Router o) {
        if (o == null) {
            throw new IllegalArgumentException();
        }
        if (this.priority == o.getPriority()) {
            if (o.getUrl() == null) {
                return -1;
            }
            return url.toFullString().compareTo(o.getUrl().toFullString());
        } else {
            return this.priority > o.getPriority() ? 1 : -1;
        }
    }
}
