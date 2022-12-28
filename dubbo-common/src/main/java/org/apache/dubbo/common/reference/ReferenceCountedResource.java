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
package org.apache.dubbo.common.reference;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_ERROR_CLOSE_CLIENT;

/**
 * inspired by Netty
 */
public abstract class ReferenceCountedResource implements AutoCloseable {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(ReferenceCountedResource.class);
    private static final AtomicLongFieldUpdater<ReferenceCountedResource> COUNTER_UPDATER
        = AtomicLongFieldUpdater.newUpdater(ReferenceCountedResource.class, "counter");

    private volatile long counter = 1;

    /**
     * Increments the reference count by 1.
     */
    public final ReferenceCountedResource retain() {
        long oldCount = COUNTER_UPDATER.getAndIncrement(this);
        if (oldCount <= 0) {
            COUNTER_UPDATER.getAndDecrement(this);
            throw new AssertionError("This instance has been destroyed");
        }
        return this;
    }

    /**
     * Decreases the reference count by 1 and calls {@link this#destroy} if the reference count reaches 0.
     */
    public final boolean release() {
        long remainingCount = COUNTER_UPDATER.decrementAndGet(this);

        if (remainingCount == 0) {
            destroy();
            return true;
        } else if (remainingCount <= -1) {
            logger.warn(PROTOCOL_ERROR_CLOSE_CLIENT, "", "", "This instance has been destroyed");
            return false;
        } else {
            return false;
        }
    }

    /**
     * Useful when used together with try-with-resources pattern
     */
    @Override
    public final void close() {
        release();
    }


    /**
     * This method will be invoked when counter reaches 0, override this method to destroy materials related to the specific resource.
     */
    protected abstract void destroy();

}
