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
package com.alibaba.dubbo.flowcontrol;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.LogHelper;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CircuitBreakerManagerDefault implements CircuitBreakerManager {

    private static final ConcurrentHashMap<String, CircuitBreaker> BREAKERS = new ConcurrentHashMap<String, CircuitBreaker>();

    private static Logger logger = LoggerFactory.getLogger(CircuitBreakerManagerDefault.class);

    private Lock openLock = new ReentrantLock();

    private Lock closeLock = new ReentrantLock();

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        URL url = invoker.getUrl();
        boolean cbSwitch = isOpenCircuitBreaker(url);
        //熔断打开
        if (cbSwitch) {
            String key = getHystrixCommandKey(invoker, invocation);
            CircuitBreaker breaker = getCircuitBreaker(url, key);
            if (breaker == null) {
                return invoker.invoke(invocation);
            }
            breaker.incrTotleCount();
            Result returnValue = null;
            /* 进入了熔断状态*/
            if (breaker.isOpen()) {
                throw new RpcException("short-circuited is opened!" + key);
            } else if (breaker.isClosed()) {
                try {
                    returnValue = invoker.invoke(invocation);
                } catch (RpcException e) {
                   /* 增加计数*/
                    breaker.incrFailCount();
                    /*达到熔断开启条件*/
                    if (breaker.getBuffStatus()&&breaker.isClosed() && breaker.closeFailThresholdReached()) {
                        if (openLock.tryLock() && breaker.isClosed()) {
                            try {
                                breaker.open();  //触发阈值，打开
                                LogHelper.error(logger, "short-circuited threshold opened " + key);
                            } finally {
                                openLock.unlock();
                            }
                        }
                        throw e;
                    } else {
                        throw e;
                    }
                }
            } else if (breaker.isHalfOpen()) {
                returnValue = processHalfOpen(breaker, invoker, invocation, key);
            }
            return returnValue;
        } else {
            return invoker.invoke(invocation);
        }
    }

    private Result processHalfOpen(CircuitBreaker breaker, Invoker<?> invoker, Invocation invocation, String key) throws RpcException {
        try {
            Result returnValue = invoker.invoke(invocation);
            int count = breaker.getConsecutiveSuccCount().incrementAndGet();
            //达到成功次数 关闭熔断
            if (breaker.isConsecutiveSuccessThresholdReached()) {
                if (!breaker.isClosed()) {
                    breaker.close();//调用成功则进入close状态
                    LogHelper.error(logger, "short-circuited ####### try success! " + count + " close success! key:" + key);
                }
            }
            return returnValue;
        } catch (RpcException e) {
            if (!breaker.isOpen() && closeLock.tryLock()) {
                try {
                    LogHelper.error(logger, "short-circuited reopen :" + key);
                    breaker.open();
                } finally {
                    closeLock.unlock();
                }
            }
            LogHelper.error(logger, "short-circuited try close fail! state:" + breaker.getState().name() + " key:" + key);
            throw e;
        }
    }

    /**
     * 是否为熔断接口 --- concurrentHashMap中存在且已经发生了熔断包括 open 和 halfopen
     *
     * @param invoker
     * @param invocation
     * @return
     */
    @Override
    public boolean isCircuitBreakerInterface(Invoker invoker, Invocation invocation) {
        try {
            if (BREAKERS == null || BREAKERS.size() == 0) {
                return false;
            }
            String commandKey = getHystrixCommandKey(invoker, invocation);
            if (StringUtils.isBlank(commandKey)) {
                return false;
            }
            if (BREAKERS.containsKey(commandKey) && !BREAKERS.get(commandKey).isClosed()) {
                return true;
            }
        } catch (Exception e) {
            LogHelper.error(logger, "isCircuitBreakerInterface", e);
        }

        return false;
    }
    @Override
    public <T> List<Invoker<T>> filterCricuitBreakInvoker(List<Invoker<T>> invokers, Invocation invocation) {
        if (invokers.size() == 0) {
            return invokers;
        }
        List<Invoker<T>> list = new ArrayList<Invoker<T>>();
        try {
            for (Invoker invoker : invokers) {
                boolean isOpen = isOpenCircuitBreaker(invoker.getUrl());
                if (isOpen) {
                    String commandKey = getHystrixCommandKey(invoker, invocation);
                    if (!isForbidInvoker(commandKey)) {
                        list.add(invoker);
                    }
                } else {
                    list.add(invoker);
                    removeCircuitBreaker(invoker, invocation);
                }
            }
        } catch (Exception e) {
            LogHelper.error(logger, "short-circuited filterCricuitBreakInvoker", e);
        }
        return list;
    }


    private String getHystrixCommandKey(Invoker<?> invoker, Invocation invocation) {
        return invoker.getUrl().getHost() + "_" + invoker.getUrl().getServiceInterface() + "_" + invoker.getUrl().getParameter("group") + "_" + invocation.getMethodName() + "_" +
                invoker.getUrl().getParameter("version") + "_" +
                (invocation.getArguments() == null ? 0 : invocation.getArguments().length);
    }

    /**
     * 是否开启了熔断机制
     *
     * @param url
     * @return
     */
    private boolean isOpenCircuitBreaker(URL url) {
        if (url == null) {
            return false;
        }
        return url.getParameter(Constants.CIRCUIT_BREAKER_SWITCH, false);
    }

    /**
     * 关闭熔断机制 后 移除熔断器
     *
     * @param invoker
     * @param invocation
     */
    private void removeCircuitBreaker(Invoker invoker, Invocation invocation) {
        try {
            String commandKey = getHystrixCommandKey(invoker, invocation);
            if (BREAKERS.containsKey(commandKey)) {
                BREAKERS.remove(commandKey);
                LogHelper.error(logger, "short-circuited circuit.breaker.switch close remove key:" + commandKey);
            }
        } catch (Exception e) {
            LogHelper.error(logger, "short-circuited  removeParmsAndCircuitBreaker", e);
        }
    }

    private boolean isForbidInvoker(String commandKey) {
        try {
            CircuitBreaker breaker = BREAKERS.get(commandKey);
            if (breaker == null || breaker.isClosed()) {
                return false;
            }
            if (breaker.isOpen()) {
                if (breaker.inSleepWindowNew()) {
                    return true;
                } else {
                    breaker.openHalf();
                    return false;
                }
            }
        } catch (Exception e) {
            LogHelper.error(logger, "short-circuited allowInvoker ", e);
        }
        return false;
    }

    /**
     * 如果存在 HalfOpen 可重试接口
     *
     * @param invoker
     * @param invocation
     * @return
     */
    @Override
    public boolean isSwitchLoadBalance(Invoker invoker, Invocation invocation) {
        try {
            String commandKey = getHystrixCommandKey(invoker, invocation);
            CircuitBreaker breaker = BREAKERS.get(commandKey);
            if (breaker != null && breaker.isHalfOpen()) {
                LogHelper.error(logger, "short-circuited isSwitchLoadBalance isHalfOpen:" + commandKey);
                return true;
            } else {
                return false;
            }

        } catch (Exception e) {
            LogHelper.error(logger, "short-circuited isSwitchLoadBalance ", e);
            return false;
        }
    }

    /**
     * 获取熔断器
     *
     * @param url
     * @param key
     * @return
     */
    private CircuitBreaker getCircuitBreaker(URL url, String key) {
        try {
            int circuitBreakerRequestVolumeThreshold = url.getParameter(Constants.CIRCUIT_BREAKER_REQUEST_VOLUME_THRESHOLD, Constants.CIRCUIT_BREAKER_REQUEST_VOLUME_THRESHOLD_DEFAULT);
            int circuitBreakerSleepWindowInMilliseconds = url.getParameter(Constants.CIRCUIT_BREAKER_SLEEP_WINDOWIN_MILLISECONDS, Constants.CIRCUIT_BREAKER_SLEEP_WINDOWIN_MILLISECONDS_DEFAULT);
            int circuitBreakerErrorThresholdPercentage = url.getParameter(Constants.CIRCUIT_BREAKER_ERROR_THRESHOLD_PERCENTAGE, Constants.CIRCUIT_BREAKER_ERROR_THRESHOLD_PERCENTAGE_DEFAULT);
            CircuitBreaker breaker = BREAKERS.get(key);
            if (breaker == null) {
                CircuitBreakerConfig cfg = CircuitBreakerConfig.newDefault();
                cfg.setCircuitBreakerRequestVolumeThreshold(circuitBreakerRequestVolumeThreshold);
                cfg.setCircuitBreakerSleepWindowInMilliseconds(circuitBreakerSleepWindowInMilliseconds);
                cfg.setCircuitBreakerErrorThresholdPercentage(circuitBreakerErrorThresholdPercentage);
                breaker = new CircuitBreaker(key, cfg);
                BREAKERS.putIfAbsent(key, breaker);
            } else {
                CircuitBreakerConfig cfg = breaker.getConfig();
                cfg.parmEqual(circuitBreakerRequestVolumeThreshold, circuitBreakerSleepWindowInMilliseconds, circuitBreakerErrorThresholdPercentage);
            }
            return breaker;
        } catch (Exception e) {
            LogHelper.error(logger, "short-circuited getCircuitBreaker() ", e);
        }
        return null;
    }
}
