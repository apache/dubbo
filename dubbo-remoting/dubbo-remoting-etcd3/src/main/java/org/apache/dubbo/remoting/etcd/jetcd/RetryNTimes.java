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
package org.apache.dubbo.remoting.etcd.jetcd;

import org.apache.dubbo.remoting.etcd.AbstractRetryPolicy;

import java.util.concurrent.TimeUnit;

public class RetryNTimes extends AbstractRetryPolicy {

    private final long sleepMilliseconds;

    public RetryNTimes(int maxRetried, int sleepTime, TimeUnit unit) {
        super(maxRetried);
        this.sleepMilliseconds = unit.convert(sleepTime, TimeUnit.MILLISECONDS);
    }

    @Override
    protected long getSleepTime(int retried, long elapsed) {
        return sleepMilliseconds;
    }
}
