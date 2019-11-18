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
package org.apache.dubbo.rpc.cluster.router.env;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.router.AbstractRouter;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.CommonConstants.ROUTER_ENV_KEY;


/**
 * EnvRouter, "application.env-router"
 */
public class EnvRouter extends AbstractRouter {
    public static final String NAME = "ENV_ROUTER";
    private static final Logger logger = LoggerFactory.getLogger(EnvRouter.class);
    private static final int ENV_ROUTER_DEFAULT_PRIORITY = 400;

    public EnvRouter(URL url) {
        super(url);
        this.priority = ENV_ROUTER_DEFAULT_PRIORITY;
    }


    @Override
    public <T> List<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        if (CollectionUtils.isEmpty(invokers)) {
            return invokers;
        }

        List<Invoker<T>> result = invokers;
        String env = url.getParameter(ROUTER_ENV_KEY);

        if (StringUtils.isNotEmpty(env)) {
            result = filterInvoker(invokers, invoker -> env.equals(invoker.getUrl().getParameter(ROUTER_ENV_KEY)));
        }

        if (CollectionUtils.isNotEmpty(result)) {
            return result;
        }  else if (force) {
            logger.warn("The route result is empty and force execute. consumer: " + NetUtils.getLocalHost() + ", service: " + url.getServiceKey() + ", env: " + env);
            return result;
        }
        return invokers;
    }

    private <T> List<Invoker<T>> filterInvoker(List<Invoker<T>> invokers, Predicate<Invoker<T>> predicate) {
        return invokers.stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

}
