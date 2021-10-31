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
import org.apache.dubbo.test.check.registrycenter.Initializer;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The implementation of {@link Initializer} to initialize zookeeper.
 */
public abstract class ZookeeperInitializer implements Initializer {

    /**
     * The default version of zookeeper.
     */
    public static final String DEFAULT_ZOOKEEPER_VERSION = "3.6.0";

    /**
     * The default client ports of zookeeper.
     */
    public static final int[] DEFAULT_CLIENT_PORTS = new int[]{2181, 2182};

    /**
     * The default admin server ports of zookeeper.
     */
    public static final int[] DEFAULT_ADMIN_SERVER_PORTS = new int[]{8081, 8082};

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

    /**
     * The global context for zookeeper.
     */
    public static class ZookeeperContext implements Initializer.Context {

        /**
         * The the source file path of downloaded zookeeper binary archive.
         */
        private Path sourceFile;

        /**
         * Sets the source file path of downloaded zookeeper binary archive.
         */
        public void setSourceFile(Path sourceFile){
            this.sourceFile = sourceFile;
        }

        /**
         * Returns the source file path of downloaded zookeeper binary archive.
         */
        public Path getSourceFile(){
            return this.sourceFile;
        }

        /**
         * Returns the zookeeper's version.
         */
        public String getVersion() {
            return DEFAULT_ZOOKEEPER_VERSION;
        }

        /**
         * Returns the client ports of zookeeper.
         */
        public int[] getClientPorts() {
            return DEFAULT_CLIENT_PORTS;
        }

        /**
         * Returns the admin server ports of zookeeper.
         */
        public int[] getAdminServerPorts() {
            return DEFAULT_ADMIN_SERVER_PORTS;
        }
    }
}
