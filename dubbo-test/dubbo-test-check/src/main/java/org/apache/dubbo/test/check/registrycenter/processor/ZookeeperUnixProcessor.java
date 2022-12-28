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

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.test.check.exception.DubboTestException;
import org.apache.dubbo.test.check.registrycenter.Context;
import org.apache.dubbo.test.check.registrycenter.Processor;
import org.apache.dubbo.test.check.registrycenter.context.ZookeeperContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.TESTING_REGISTRY_FAILED_TO_START_ZOOKEEPER;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.TESTING_REGISTRY_FAILED_TO_STOP_ZOOKEEPER;

/**
 * The abstract implementation of {@link Processor} is to provide some common methods on Unix OS.
 */
public abstract class ZookeeperUnixProcessor implements Processor {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(ZookeeperUnixProcessor.class);

    @Override
    public void process(Context context) throws DubboTestException {
        ZookeeperContext zookeeperContext = (ZookeeperContext) context;
        for (int clientPort : zookeeperContext.getClientPorts()) {
            Process process = this.doProcess(zookeeperContext, clientPort);
            this.logErrorStream(process.getErrorStream());
            this.awaitProcessReady(process.getInputStream());
            // kill the process
            try {
                process.destroy();
            } catch (Throwable cause) {
                logger.warn(TESTING_REGISTRY_FAILED_TO_STOP_ZOOKEEPER, "", "", String.format("Failed to kill the process, with client port %s !", clientPort), cause);
            }
        }
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
                logger.error(TESTING_REGISTRY_FAILED_TO_START_ZOOKEEPER,"","",line);
            }
        } catch (IOException e) {
            /* eat quietly */
        }
    }

    /**
     * Wait until the server is started successfully.
     *
     * @param inputStream the log after run {@link Process}.
     * @throws DubboTestException if cannot match the given pattern.
     */
    private void awaitProcessReady(final InputStream inputStream) throws DubboTestException {
        final StringBuilder log = new StringBuilder();
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (this.getPattern().matcher(line).matches()) {
                    return;
                }
                log.append('\n').append(line);
            }
        } catch (IOException e) {
            throw new DubboTestException("Failed to read the log after executed process.", e);
        }
        throw new DubboTestException("Ready pattern not found in log, log: " + log);
    }

    /**
     * Use {@link Process} to handle the command.
     *
     * @param context    the global zookeeper context.
     * @param clientPort the client port of zookeeper.
     * @return the instance of {@link Process}.
     * @throws DubboTestException when any exception occurred.
     */
    protected abstract Process doProcess(ZookeeperContext context, int clientPort) throws DubboTestException;

    /**
     * Gets the pattern to check the server is ready or not.
     *
     * @return the pattern for checking.
     */
    protected abstract Pattern getPattern();
}
