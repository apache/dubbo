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
package org.apache.dubbo.common.utils;

import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;

public class LockUtils {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(LockUtils.class);

    public static final int DEFAULT_TIMEOUT = 60_000;

    public static void safeLock(Lock lock, int timeout, Runnable runnable) {
        try {
            if (!lock.tryLock(timeout, TimeUnit.MILLISECONDS)) {
                logger.error(
                        LoggerCodeConstants.INTERNAL_ERROR,
                        "",
                        "",
                        "Try to lock failed, timeout: " + timeout,
                        new TimeoutException());
            }
            runnable.run();
        } catch (InterruptedException e) {
            logger.warn(LoggerCodeConstants.INTERNAL_ERROR, "", "", "Try to lock failed", e);
            Thread.currentThread().interrupt();
        } finally {
            try {
                lock.unlock();
            } catch (Exception e) {
                // ignore
            }
        }
    }
}
