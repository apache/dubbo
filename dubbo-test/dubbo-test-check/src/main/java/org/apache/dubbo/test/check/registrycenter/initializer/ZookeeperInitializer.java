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
package org.apache.dubbo.test.check.registrycenter.initializer;

import org.apache.dubbo.test.check.exception.DubboTestException;
import org.apache.dubbo.test.check.registrycenter.Context;
import org.apache.dubbo.test.check.registrycenter.Initializer;
import org.apache.dubbo.test.check.registrycenter.context.ZookeeperContext;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The implementation of {@link Initializer} to initialize zookeeper.
 */
public abstract class ZookeeperInitializer implements Initializer {

    /**
     * The {@link #INITIALIZED} for checking the {@link #initialize(Context)} method is called or not.
     */
    private final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    @Override
    public void initialize(Context context) throws DubboTestException {
        if (!this.INITIALIZED.compareAndSet(false, true)) {
            return;
        }
        this.doInitialize((ZookeeperContext) context);
    }

    /**
     * Initialize the global context for zookeeper.
     *
     * @param context the global context for zookeeper.
     * @throws DubboTestException when any exception occurred.
     */
    protected abstract void doInitialize(ZookeeperContext context) throws DubboTestException;
}
