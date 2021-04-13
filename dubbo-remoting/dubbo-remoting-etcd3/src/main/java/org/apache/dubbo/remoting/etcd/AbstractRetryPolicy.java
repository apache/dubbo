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
package org.apache.dubbo.remoting.etcd;

public abstract class AbstractRetryPolicy implements RetryPolicy {

    private final int maxRetried;

    protected AbstractRetryPolicy(int maxRetried) {
        this.maxRetried = maxRetried;
    }

    @Override
    public boolean shouldRetry(int retried, long elapsed, boolean sleep) {
        if (retried < maxRetried) {
            try {
                if (sleep) {
                    Thread.sleep(getSleepTime(retried, elapsed));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
            return true;
        }
        return false;
    }

    protected abstract long getSleepTime(int retried, long elapsed);

}
