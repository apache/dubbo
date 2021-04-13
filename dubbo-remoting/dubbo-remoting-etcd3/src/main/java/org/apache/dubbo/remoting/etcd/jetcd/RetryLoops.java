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

import io.grpc.Status;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.etcd.RetryPolicy;
import org.apache.dubbo.remoting.etcd.option.OptionUtil;

import java.util.concurrent.Callable;

public class RetryLoops {

    private final long startTimeMs = System.currentTimeMillis();
    private boolean isDone = false;
    private int retriedCount = 0;
    private Logger logger = LoggerFactory.getLogger(RetryLoops.class);

    public static <R> R invokeWithRetry(Callable<R> task, RetryPolicy retryPolicy) throws Exception {
        R result = null;
        RetryLoops retryLoop = new RetryLoops();
        while (retryLoop.shouldContinue()) {
            try {
                result = task.call();
                retryLoop.complete();
            } catch (Exception e) {
                retryLoop.fireException(e, retryPolicy);
            }
        }
        return result;
    }

    public void fireException(Exception e, RetryPolicy retryPolicy) throws Exception {

        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }

        boolean rethrow = true;
        if (isRetryException(e)
                && retryPolicy.shouldRetry(retriedCount++, System.currentTimeMillis() - startTimeMs, true)) {
            rethrow = false;
        }

        if (rethrow) {
            throw e;
        }
    }

    private boolean isRetryException(Throwable e) {
        Status status = Status.fromThrowable(e);
        if (OptionUtil.isRecoverable(status)) {
            return true;
        }

        return false;
    }

    public boolean shouldContinue() {
        return !isDone;
    }

    public void complete() {
        isDone = true;
    }

}
