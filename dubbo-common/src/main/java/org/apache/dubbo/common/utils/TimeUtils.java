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

import java.util.concurrent.TimeUnit;

/**
 * Provide currentTimeMillis acquisition for high-frequency access scenarios.
 */
public final class TimeUtils {

    private static volatile long currentTimeMillis;

    private static volatile boolean isTickerAlive = false;

    private static volatile boolean isFallback = false;

    private TimeUtils() {
    }

    public static long currentTimeMillis() {
        // When an exception occurs in the Ticker mechanism, fall back.
        if (isFallback) {
            return System.currentTimeMillis();
        }

        if (!isTickerAlive) {
            try {
                startTicker();
            } catch (Exception e) {
                isFallback = true;
            }
        }
        return currentTimeMillis;
    }

    private static synchronized void startTicker() {
        if (!isTickerAlive) {
            currentTimeMillis = System.currentTimeMillis();
            Thread ticker = new Thread(() -> {
                while (isTickerAlive) {
                    currentTimeMillis = System.currentTimeMillis();
                    try {
                        TimeUnit.MILLISECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        isTickerAlive = false;
                        Thread.currentThread().interrupt();
                    } catch (Exception ignored) {
                        //
                    }
                }
            });
            ticker.setDaemon(true);
            ticker.setName("time-millis-ticker-thread");
            ticker.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                isFallback = true;
                ticker.interrupt();
            }));
            isTickerAlive = true;
        }
    }
}
