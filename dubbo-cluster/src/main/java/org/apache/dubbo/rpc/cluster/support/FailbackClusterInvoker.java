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
package org.apache.dubbo.rpc.cluster.support;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.timer.HashedWheelTimer;
import org.apache.dubbo.common.timer.Timeout;
import org.apache.dubbo.common.timer.Timer;
import org.apache.dubbo.common.timer.TimerTask;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcResult;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.LoadBalance;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * When fails, record failure requests and schedule for retry on a regular interval.
 * Especially useful for services of notification.
 *
 * <a href="http://en.wikipedia.org/wiki/Failback">Failback</a>
 */
public class FailbackClusterInvoker<T> extends AbstractClusterInvoker<T> {

    private static final Logger logger = LoggerFactory.getLogger(FailbackClusterInvoker.class);

    private static final long RETRY_FAILED_PERIOD = 5;

    private final int retries;

    private final int failCapacitySize;

    private volatile Timer failTimer;

    public FailbackClusterInvoker(Directory<T> directory) {
        super(directory);

        int retriesConfig = getUrl().getParameter(Constants.RETRIES_KEY, Constants.DEFAULT_FAIL_RETRY_SIZE) + 1;
        if (retriesConfig <= 0) {
            retriesConfig = Constants.DEFAULT_FAIL_RETRY_SIZE;
        }
        int failCapacitySizeConfig = getUrl().getParameter(Constants.FAIL_CAPACITY_KEY, Constants.DEFAULT_FAIL_CAPACITY_SIZE);
        if (failCapacitySizeConfig <= 0) {
            failCapacitySizeConfig = Constants.DEFAULT_FAIL_RETRY_SIZE;
        }
        retries = retriesConfig;
        failCapacitySize = failCapacitySizeConfig;
    }

    private void addFailed(Invocation invocation, AbstractClusterInvoker<?> router) {
        if (failTimer == null) {
            synchronized (this) {
                if (failTimer == null) {
                    failTimer = new HashedWheelTimer(
                            new NamedThreadFactory("failback-cluster-timer", true),
                            1,
                            TimeUnit.SECONDS, 32, failCapacitySize);
                }
            }
        }
        RetryTimerTask retryTimerTask = new RetryTimerTask(invocation, router, retries, RETRY_FAILED_PERIOD);
        try {
            failTimer.newTimeout(retryTimerTask, RETRY_FAILED_PERIOD, TimeUnit.SECONDS);
        } catch (Throwable e) {
            logger.error("Failback background works error,invocation->" + invocation + ", exception: " + e.getMessage());
        }
    }

    @Override
    protected Result doInvoke(Invocation invocation, List<Invoker<T>> invokers, LoadBalance loadbalance) throws RpcException {
        try {
            checkInvokers(invokers, invocation);
            Invoker<T> invoker = select(loadbalance, invocation, invokers, null);
            return invoker.invoke(invocation);
        } catch (Throwable e) {
            logger.error("Failback to invoke method " + invocation.getMethodName() + ", wait for retry in background. Ignored exception: "
                    + e.getMessage() + ", ", e);
            addFailed(invocation, this);
            return new RpcResult(); // ignore
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (failTimer != null) {
            failTimer.stop();
        }
    }

    /**
     * RetryTimerTask
     */
    private static class RetryTimerTask implements TimerTask {
        private final Invocation invocation;
        private final AbstractClusterInvoker<?> router;
        private final int retries;
        private final long tick;
        private final AtomicInteger retryTimes = new AtomicInteger(0);

        RetryTimerTask(Invocation invocation, AbstractClusterInvoker<?> router, int retries, long tick) {
            this.invocation = invocation;
            this.router = router;
            this.retries = retries;
            this.tick = tick;
        }

        @Override
        public void run(Timeout timeout) throws Exception {
            try {
                router.invoke(invocation);
            } catch (Throwable e) {
                logger.error("Failed retry to invoke method " + invocation.getMethodName() + ", waiting again.", e);
                if (retryTimes.incrementAndGet() >= retries) {
                    logger.error("Failed retry times exceed threshold (" + retries + "), We have to abandon, invocation->" + invocation);
                } else {
                    rePut(timeout);
                }
            }
        }

        private void rePut(Timeout timeout) {
            if (timeout == null) {
                return;
            }

            Timer timer = timeout.timer();
            if (timer.isStop() || timeout.isCancelled()) {
                return;
            }

            timer.newTimeout(timeout.task(), tick, TimeUnit.SECONDS);
        }
    }
}
