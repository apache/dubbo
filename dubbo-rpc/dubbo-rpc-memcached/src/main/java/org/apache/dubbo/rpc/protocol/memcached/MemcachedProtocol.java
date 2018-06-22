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
package org.apache.dubbo.rpc.protocol.memcached;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcResult;
import org.apache.dubbo.rpc.protocol.AbstractInvoker;
import org.apache.dubbo.rpc.protocol.AbstractProtocol;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.utils.AddrUtil;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * MemcachedProtocol
 */
public class MemcachedProtocol extends AbstractProtocol {

    public static final int DEFAULT_PORT = 11211;

    @Override
    public int getDefaultPort() {
        return DEFAULT_PORT;
    }

    @Override
    public <T> Exporter<T> export(final Invoker<T> invoker) throws RpcException {
        throw new UnsupportedOperationException("Unsupported export memcached service. url: " + invoker.getUrl());
    }

    @Override
    public <T> Invoker<T> refer(final Class<T> type, final URL url) throws RpcException {
        try {
            String address = url.getAddress();
            String backup = url.getParameter(Constants.BACKUP_KEY);
            if (backup != null && backup.length() > 0) {
                address += "," + backup;
            }
            MemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil.getAddresses(address));
            final MemcachedClient memcachedClient = builder.build();
            final int expiry = url.getParameter("expiry", 0);
            final String get = url.getParameter("get", "get");
            final String set = url.getParameter("set", Map.class.equals(type) ? "put" : "set");
            final String delete = url.getParameter("delete", Map.class.equals(type) ? "remove" : "delete");
            return new AbstractInvoker<T>(type, url) {
                @Override
                protected Result doInvoke(Invocation invocation) throws Throwable {
                    try {
                        if (get.equals(invocation.getMethodName())) {
                            if (invocation.getArguments().length != 1) {
                                throw new IllegalArgumentException("The memcached get method arguments mismatch, must only one arguments. interface: " + type.getName() + ", method: " + invocation.getMethodName() + ", url: " + url);
                            }
                            return new RpcResult(memcachedClient.get(String.valueOf(invocation.getArguments()[0])));
                        } else if (set.equals(invocation.getMethodName())) {
                            if (invocation.getArguments().length != 2) {
                                throw new IllegalArgumentException("The memcached set method arguments mismatch, must be two arguments. interface: " + type.getName() + ", method: " + invocation.getMethodName() + ", url: " + url);
                            }
                            memcachedClient.set(String.valueOf(invocation.getArguments()[0]), expiry, invocation.getArguments()[1]);
                            return new RpcResult();
                        } else if (delete.equals(invocation.getMethodName())) {
                            if (invocation.getArguments().length != 1) {
                                throw new IllegalArgumentException("The memcached delete method arguments mismatch, must only one arguments. interface: " + type.getName() + ", method: " + invocation.getMethodName() + ", url: " + url);
                            }
                            memcachedClient.delete(String.valueOf(invocation.getArguments()[0]));
                            return new RpcResult();
                        } else {
                            throw new UnsupportedOperationException("Unsupported method " + invocation.getMethodName() + " in memcached service.");
                        }
                    } catch (Throwable t) {
                        RpcException re = new RpcException("Failed to invoke memcached service method. interface: " + type.getName() + ", method: " + invocation.getMethodName() + ", url: " + url + ", cause: " + t.getMessage(), t);
                        if (t instanceof TimeoutException || t instanceof SocketTimeoutException) {
                            re.setCode(RpcException.TIMEOUT_EXCEPTION);
                        } else if (t instanceof MemcachedException || t instanceof IOException) {
                            re.setCode(RpcException.NETWORK_EXCEPTION);
                        }
                        throw re;
                    }
                }

                @Override
                public void destroy() {
                    super.destroy();
                    try {
                        memcachedClient.shutdown();
                    } catch (Throwable e) {
                        logger.warn(e.getMessage(), e);
                    }
                }
            };
        } catch (Throwable t) {
            throw new RpcException("Failed to refer memcached service. interface: " + type.getName() + ", url: " + url + ", cause: " + t.getMessage(), t);
        }
    }

}
