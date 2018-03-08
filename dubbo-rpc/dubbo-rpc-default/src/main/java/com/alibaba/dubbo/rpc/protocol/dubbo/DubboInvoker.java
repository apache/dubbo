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
package com.alibaba.dubbo.rpc.protocol.dubbo;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alibaba.dubbo.cache.CacheService;
import com.alibaba.dubbo.cache.DubboCacheFactory;
import com.alibaba.dubbo.cache.common.DubboBuffer;
import com.alibaba.dubbo.cache.common.DubboCache;
import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.AtomicPositiveInteger;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.TimeoutException;
import com.alibaba.dubbo.remoting.exchange.ExchangeClient;
import com.alibaba.dubbo.remoting.exchange.ResponseFuture;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.RpcResult;
import com.alibaba.dubbo.rpc.protocol.AbstractInvoker;
import com.alibaba.dubbo.rpc.support.RpcUtils;

/**
 * DubboInvoker
 */
public class DubboInvoker<T> extends AbstractInvoker<T> {

    private final ExchangeClient[] clients;

    private final AtomicPositiveInteger index = new AtomicPositiveInteger();

    private final String version;

    private final ReentrantLock destroyLock = new ReentrantLock();

    private final Set<Invoker<?>> invokers;

    private DubboCache dubboCache;//skykong1981
    
    public DubboInvoker(Class<T> serviceType, URL url, ExchangeClient[] clients) {
        this(serviceType, url, clients, null);
    }

    public DubboInvoker(Class<T> serviceType, URL url, ExchangeClient[] clients, Set<Invoker<?>> invokers) {
        super(serviceType, url, new String[]{Constants.INTERFACE_KEY, Constants.GROUP_KEY, Constants.TOKEN_KEY, Constants.TIMEOUT_KEY});
        this.clients = clients;
        // get version.
        this.version = url.getParameter(Constants.VERSION_KEY, "0.0.0");
        this.invokers = invokers;
        //skykong1981
        String cacheData = url.getData();
        if (cacheData != null && !cacheData.isEmpty()) {
        	dubboCache = new DubboCache(cacheData);
        }
    }

    @Override
    protected Result doInvoke(final Invocation invocation) throws Throwable {
        RpcInvocation inv = (RpcInvocation) invocation;
        final String methodName = RpcUtils.getMethodName(invocation);
        inv.setAttachment(Constants.PATH_KEY, getUrl().getPath());
        inv.setAttachment(Constants.VERSION_KEY, version);
        
        //skykong1981
        Object[] args = invocation.getArguments();        
        boolean needRemoteCall = true;
        boolean needRemoteCache = false;
        String cacheKey = null;
        Object cacheValue = null;
        int cacheTimeout = 0;
        CacheService cacheService = null;
        if (dubboCache != null) {
    		try {
    			cacheService = DubboCacheFactory.getCacheService(dubboCache);
            	if (cacheService != null && cacheService.isConnected()) {
            		Map<String, DubboBuffer> bufferMap = dubboCache.getBufferMap();
            		DubboBuffer buffer = bufferMap.get(methodName);
            		if (buffer != null) {//configed cache in this function
                		String command = buffer.getCommand();
                		if (command.equals("get")) {
                			String key = buffer.getKey();
                			cacheKey = getCacheKey(key, args);
                			cacheValue = cacheService.get(cacheKey);
                			cacheTimeout = buffer.getTimeout();
                			if (cacheValue != null) {
                				if (logger.isDebugEnabled()) {
                					logger.debug("Cache contains key : " + cacheKey);
                				}
                				needRemoteCall = false;
                			} else {
                				if (logger.isDebugEnabled()) {
                					logger.debug("Cache doesn't contain key : " + cacheKey);
                				}
                				needRemoteCache = true;
                			}
                		} else {//command is delete
                			String[] keys = buffer.getKeys();
                			for (String key : keys) {
                    			cacheKey = getCacheKey(key, args);
                    			cacheService.delete(cacheKey);
                			}
                		}
            		}
            	}
    		} catch (Exception e) {
    			logger.warn("Get result from remote cache error!", e);
    			needRemoteCall = true;
    	        needRemoteCache = false;
    		}
    	}
        
        if (!needRemoteCall) {
        	return new RpcResult(cacheValue);
        }
        //skykong1981

        ExchangeClient currentClient;
        if (clients.length == 1) {
            currentClient = clients[0];
        } else {
            currentClient = clients[index.getAndIncrement() % clients.length];
        }
        try {
            boolean isAsync = RpcUtils.isAsync(getUrl(), invocation);
            boolean isOneway = RpcUtils.isOneway(getUrl(), invocation);
            int timeout = getUrl().getMethodParameter(methodName, Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
            if (isOneway) {
                boolean isSent = getUrl().getMethodParameter(methodName, Constants.SENT_KEY, false);
                currentClient.send(inv, isSent);
                RpcContext.getContext().setFuture(null);
                return new RpcResult();
            } else if (isAsync) {
                ResponseFuture future = currentClient.request(inv, timeout);
                RpcContext.getContext().setFuture(new FutureAdapter<Object>(future));
                return new RpcResult();
            } else {
                RpcContext.getContext().setFuture(null);
                //return (Result) currentClient.request(inv, timeout).get();
                //skykong1981
            	RpcResult rpcResult = (RpcResult) currentClient.request(inv, timeout).get();
            	if (needRemoteCache) {
            		Object value = rpcResult.getValue();
            		if (value != null) {
            			if (cacheTimeout == 0) {
            				cacheService.put(cacheKey, value);
            			} else {
            				cacheService.put(cacheKey, value, cacheTimeout);
            			}
            		}
            	}
            	return rpcResult;
            	//skykong1981
            }
        } catch (TimeoutException e) {
            throw new RpcException(RpcException.TIMEOUT_EXCEPTION, "Invoke remote method timeout. method: " + invocation.getMethodName() + ", provider: " + getUrl() + ", cause: " + e.getMessage(), e);
        } catch (RemotingException e) {
            throw new RpcException(RpcException.NETWORK_EXCEPTION, "Failed to invoke remote method: " + invocation.getMethodName() + ", provider: " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        if (!super.isAvailable())
            return false;
        for (ExchangeClient client : clients) {
            if (client.isConnected() && !client.hasAttribute(Constants.CHANNEL_ATTRIBUTE_READONLY_KEY)) {
                //cannot write == not Available ?
                return true;
            }
        }
        return false;
    }

    public void destroy() {
        // in order to avoid closing a client multiple times, a counter is used in case of connection per jvm, every
        // time when client.close() is called, counter counts down once, and when counter reaches zero, client will be
        // closed.
        if (super.isDestroyed()) {
            return;
        } else {
            // double check to avoid dup close
            destroyLock.lock();
            try {
                if (super.isDestroyed()) {
                    return;
                }
                super.destroy();
                if (invokers != null) {
                    invokers.remove(this);
                }
                for (ExchangeClient client : clients) {
                    try {
                        client.close(ConfigUtils.getServerShutdownTimeout());
                    } catch (Throwable t) {
                        logger.warn(t.getMessage(), t);
                    }
                }
                //skykong1981
                if (dubboCache != null) {
                	try {
                		CacheService cacheService = DubboCacheFactory.getCacheService(dubboCache);
                		if (cacheService != null) {
                			cacheService.shutdown();
                			DubboCacheFactory.removeCacheService(dubboCache);
                		}
                	} catch (Throwable t) {
                        logger.warn(t.getMessage(), t);
                    }
                }
                //skykong1981
            } finally {
                destroyLock.unlock();
            }
        }
    }
    
    private String getCacheKey(String key, Object[] args) throws Exception {
        String regex = "\\{(.*?)\\}";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(key);
        while (m.find()) {
            String k = m.group();
            if (k.indexOf(".") == -1) {
            	int idx = Integer.parseInt(k.substring(1, k.length() - 1));
            	key = key.replaceFirst("\\{" + idx + "\\}", String.valueOf(args[idx - 1]));
            } else {
            	String nk = k.substring(1, k.length() - 1);//1.userid
            	String[] nkAry = nk.split("\\.");
            	int idx = Integer.parseInt(nkAry[0]);//1
            	String fieldName = nkAry[1];//userid
            	Object paramObj = args[idx - 1];//User
            	
            	Class<? extends Object> clazz = paramObj.getClass();
            	Field field = clazz.getDeclaredField(fieldName);//userid
    			field.setAccessible(true);
    			String paramValue = String.valueOf(field.get(paramObj));//userid's value
    			key = key.replaceFirst("\\{" + nk + "\\}", paramValue);
        		
            }
        }
        return dubboCache.getPrefix() + key;
    }
    
}