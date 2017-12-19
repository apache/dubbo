/*
 * Copyright 1999-2012 Alibaba Group.
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
package com.alibaba.dubbo.rpc.protocol.redis;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.serialize.Serialization;
import com.alibaba.dubbo.rpc.*;
import com.alibaba.dubbo.rpc.protocol.AbstractInvoker;
import com.alibaba.dubbo.rpc.protocol.AbstractProtocol;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;


/**
 * RedisProtocol
 *
 * @author william.liangf
 */
public class RedisProtocol extends AbstractProtocol {

    public static final int DEFAULT_PORT = 6379;

    @Override
    public int getDefaultPort() {
        return DEFAULT_PORT;
    }

    @Override
    public <T> Exporter<T> export(final Invoker<T> invoker) throws RpcException {
        throw new UnsupportedOperationException("Unsupported export redis service. url: " + invoker.getUrl());
    }

    private Serialization getSerialization(URL url) {
        return ExtensionLoader.getExtensionLoader(Serialization.class).getExtension(url.getParameter(Constants.SERIALIZATION_KEY, "java"));
    }

    @Override
    public <T> Invoker<T> refer(final Class<T> type, final URL url) throws RpcException {
        try {
            GenericObjectPoolConfig config = new GenericObjectPoolConfig();
            config.setTestOnBorrow(url.getParameter("test.on.borrow", true));
            config.setTestOnReturn(url.getParameter("test.on.return", false));
            config.setTestWhileIdle(url.getParameter("test.while.idle", false));
            if (url.getParameter("max.idle", 0) > 0)
                config.setMaxIdle(url.getParameter("max.idle", 0));
            if (url.getParameter("min.idle", 0) > 0)
                config.setMinIdle(url.getParameter("min.idle", 0));
            if (url.getParameter("max.active", 0) > 0)
                config.setMaxTotal(url.getParameter("max.active", 0));
            if (url.getParameter("max.total", 0) > 0)
                config.setMaxTotal(url.getParameter("max.total", 0));
            if (url.getParameter("max.wait", 0) > 0)
                config.setMaxWaitMillis(url.getParameter("max.wait", 0));
            if (url.getParameter("num.tests.per.eviction.run", 0) > 0)
                config.setNumTestsPerEvictionRun(url.getParameter("num.tests.per.eviction.run", 0));
            if (url.getParameter("time.between.eviction.runs.millis", 0) > 0)
                config.setTimeBetweenEvictionRunsMillis(url.getParameter("time.between.eviction.runs.millis", 0));
            if (url.getParameter("min.evictable.idle.time.millis", 0) > 0)
                config.setMinEvictableIdleTimeMillis(url.getParameter("min.evictable.idle.time.millis", 0));
            final JedisPool jedisPool = new JedisPool(config, url.getHost(), url.getPort(DEFAULT_PORT),
                    url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT));
            return new AbstractInvoker<T>(type, url) {
                @Override
                protected Result doInvoke(Invocation invocation) throws Throwable {
                    Jedis resource = null;
                    try {
                        resource = jedisPool.getResource();

                        Class<Jedis> jedisClass = Jedis.class;
                        Method method;
                        try {
                            method =
                                    jedisClass.getMethod(invocation.getMethodName(),invocation.getParameterTypes());
                        } catch (NoSuchMethodException e) {
                            throw new UnsupportedOperationException("Unsupported method " + invocation.getMethodName() + " in redis service.");
                        }
                        Object result = method.invoke(resource, invocation.getArguments());
                        if (result == null) {
                            return new RpcResult();
                        }
                        return new RpcResult(result);
                    } catch (Throwable t) {
                        RpcException re = new RpcException("Failed to invoke redis service method. interface: " + type.getName() + ", method: " + invocation.getMethodName() + ", url: " + url + ", cause: " + t.getMessage(), t);
                        if (t instanceof TimeoutException || t instanceof SocketTimeoutException) {
                            re.setCode(RpcException.TIMEOUT_EXCEPTION);
                        } else if (t instanceof JedisConnectionException || t instanceof IOException) {
                            re.setCode(RpcException.NETWORK_EXCEPTION);
                        } else if (t instanceof JedisDataException) {
                            re.setCode(RpcException.SERIALIZATION_EXCEPTION);
                        }
                        throw re;
                    } finally {
                        if (resource != null) {
                            try {
                                jedisPool.returnResource(resource);
                            } catch (Throwable t) {
                                logger.warn("returnResource error: " + t.getMessage(), t);
                            }
                        }
                    }
                }

                @Override
                public void destroy() {
                    super.destroy();
                    try {
                        jedisPool.destroy();
                    } catch (Throwable e) {
                        logger.warn(e.getMessage(), e);
                    }
                }
            };
        } catch (Throwable t) {
            throw new RpcException("Failed to refer redis service. interface: " + type.getName() + ", url: " + url + ", cause: " + t.getMessage(), t);
        }
    }

}
