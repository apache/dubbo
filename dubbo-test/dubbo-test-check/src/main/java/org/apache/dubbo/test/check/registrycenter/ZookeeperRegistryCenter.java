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
package org.apache.dubbo.test.check.registrycenter;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.test.check.exception.DubboTestException;
import org.apache.dubbo.test.check.registrycenter.initializer.ConfigZookeeperInitializer;
import org.apache.dubbo.test.check.registrycenter.initializer.DownloadZookeeperInitializer;
import org.apache.dubbo.test.check.registrycenter.initializer.UnpackZookeeperInitializer;
import org.apache.dubbo.test.check.registrycenter.initializer.ZookeeperInitializer;
import org.apache.dubbo.test.check.registrycenter.processor.ResetZookeeperProcessor;
import org.apache.dubbo.test.check.registrycenter.processor.StartZookeeperCommandProcessor;
import org.apache.dubbo.test.check.registrycenter.processor.StopZookeeperCommandProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Build the registry center with embedded zookeeper, which is run by a new process.
 */
class ZookeeperRegistryCenter implements RegistryCenter {

    public ZookeeperRegistryCenter() {
        this.initializers = new ArrayList<>();
        this.initializers.add(new DownloadZookeeperInitializer());
        this.initializers.add(new UnpackZookeeperInitializer());
        this.initializers.add(new ConfigZookeeperInitializer());
        this.startZookeeperProcessor = new StartZookeeperCommandProcessor();
        this.resetZookeeperProcessor = new ResetZookeeperProcessor();
        this.stopZookeeperProcessor = new StopZookeeperCommandProcessor();
    }

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperRegistryCenter.class);

    /**
     * All of {@link ZookeeperInitializer} instances.
     */
    private List<Initializer> initializers;

    /**
     * The global context of zookeeper.
     */
    private ZookeeperInitializer.ZookeeperContext context = new ZookeeperInitializer.ZookeeperContext();

    /**
     * Define the {@link Processor} to start zookeeper instances.
     */
    private Processor startZookeeperProcessor;

    /**
     * Define the {@link Processor} to reset zookeeper instances.
     */
    private Processor resetZookeeperProcessor;

    /**
     * Define the {@link Processor} to stop zookeeper instances.
     */
    private Processor stopZookeeperProcessor;

    /**
     * The {@link #INITIALIZED} for flagging the {@link #startup()} method is called or not.
     */
    private final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    /**
     * Returns the Operating System's name.
     */
    private String getOSName() {
        String os = System.getProperty("os.name").toLowerCase();
        String binaryVersion = "linux";
        if (os.contains("mac")) {
            binaryVersion = "darwin";
        } else if (os.contains("windows")) {
            binaryVersion = "windows";
        }
        return binaryVersion;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startup() throws DubboTestException {
        if (!this.INITIALIZED.get()) {
            if (!this.INITIALIZED.compareAndSet(false, true)) {
                return;
            }
            for (Initializer initializer : this.initializers) {
                initializer.initialize(this.context);
            }
        }
        this.startZookeeperProcessor.process(this.context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() throws DubboTestException {
        this.resetZookeeperProcessor.process(this.context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() throws DubboTestException {
        this.stopZookeeperProcessor.process(this.context);
    }
}
