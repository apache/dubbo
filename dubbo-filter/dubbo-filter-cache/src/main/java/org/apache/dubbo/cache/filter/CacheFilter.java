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
package org.apache.dubbo.cache.filter;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.apache.dubbo.cache.Cache;
import org.apache.dubbo.cache.CacheFactory;
import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

/**
 * CacheFilter
 */
@Activate(group = {Constants.CONSUMER, Constants.PROVIDER}, value = Constants.CACHE_KEY)
public class CacheFilter implements Filter {

    private CacheFactory cacheFactory;

    public void setCacheFactory(CacheFactory cacheFactory) {
        this.cacheFactory = cacheFactory;
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (cacheFactory != null && ConfigUtils.isNotEmpty(invoker.getUrl().getMethodParameter(invocation.getMethodName(), Constants.CACHE_KEY))) {
            Cache cache = cacheFactory.getCache(invoker.getUrl(), invocation);
            if (cache != null) {
                String key = StringUtils.toArgumentString(invocation.getArguments());
                FutureTask f = (FutureTask) cache.get(key);
                if (f != null) {
                    return getCacheValue(f, cache, key);
                }
                Callable<Result> inv = new Callable() {
                    @Override
                    public Result call() throws Exception {
                        return invoker.invoke(invocation);
                    }
                };
                FutureTask task = new FutureTask(inv);
                f = (FutureTask) cache.putIfAbsent(key, task);
                if (f == null) {
                    f = task;
                    f.run();
                }
                return getCacheValue(f, cache, key);
            }
        }
        return invoker.invoke(invocation);
    }

    private Result getCacheValue(final FutureTask<Result> task, final Cache cache, final Object key) {
        try {
            Result value = task.get();
            if (value.hasException()) {
                cache.remove(key);
            }
            return value;
        } catch (Exception e) {
            //invoker异常不会放进缓存，如果异步获取结果抛异常则跟结果本身无关，可能是线程中断，因此不需要移除缓存
        }
        return null;
    }
}
