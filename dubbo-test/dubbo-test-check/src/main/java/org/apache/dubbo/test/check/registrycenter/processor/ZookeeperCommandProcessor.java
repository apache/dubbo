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
package org.apache.dubbo.test.check.registrycenter.processor;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.test.check.exception.DubboTestException;
import org.apache.dubbo.test.check.registrycenter.Initializer;
import org.apache.dubbo.test.check.registrycenter.Processor;
import org.apache.dubbo.test.check.registrycenter.initializer.ZookeeperInitializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * The abstract implementation of {@link Processor} is to provide some common methods.
 */
public abstract class ZookeeperCommandProcessor implements Processor {

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperCommandProcessor.class);

    /**
     * Returns the Operating System's name.
     */
    protected String getOSName() {
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
     * Prints the error log after run {@link Process}.
     *
     * @param errorStream the error stream.
     */
    private void logErrorStream(final InputStream errorStream) {
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.error(line);
            }
        } catch (IOException e) {
            /* eat quietly */
        }
    }

    /**
     * Wait until the server is started successfully.
     *
     * @param context     the global context of zookeeper.
     * @param inputStream the log after run {@link Process}.
     * @throws DubboTestException if cannot match the given pattern.
     */
    private void awaitProcessReady(ZookeeperInitializer.ZookeeperContext context, final InputStream inputStream) throws DubboTestException {
        final StringBuilder log = new StringBuilder();
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (this.check(line))
                    return;
                log.append('\n').append(line);
            }
        } catch (IOException e) {
            throw new DubboTestException("Failed to read the log after executed process.", e);
        }
        throw new DubboTestException("Ready pattern not found in log, log: " + log);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void process(Initializer.Context context) throws DubboTestException {
        ZookeeperInitializer.ZookeeperContext zookeeperContext = (ZookeeperInitializer.ZookeeperContext) context;
        for (int clientPort : zookeeperContext.getClientPorts()) {
            Process process = this.start(zookeeperContext, clientPort);
            this.logErrorStream(process.getErrorStream());
            this.awaitProcessReady(zookeeperContext, process.getInputStream());
        }
    }

    /**
     * Starts the {@link Process} with the given {@link org.apache.dubbo.test.check.registrycenter.Initializer.Context}
     *
     * @param context    the global context of zookeeper.
     * @param clientPort the client port of zookeeper.
     * @return the executed process.
     */
    protected abstract Process start(ZookeeperInitializer.ZookeeperContext context, int clientPort);

    /**
     * Checks the server is ready or not.
     *
     * @param message the message to verify.
     * @return {@code true} if the server is ready, otherwise {@code false}.
     */
    protected abstract boolean check(String message);
}
