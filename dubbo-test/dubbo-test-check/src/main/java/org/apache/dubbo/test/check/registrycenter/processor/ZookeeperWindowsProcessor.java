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

import org.apache.dubbo.test.check.exception.DubboTestException;
import org.apache.dubbo.test.check.registrycenter.Context;
import org.apache.dubbo.test.check.registrycenter.Processor;
import org.apache.dubbo.test.check.registrycenter.context.ZookeeperWindowsContext;
/**
 * The abstract implementation of {@link Processor} is to provide some common methods on Windows OS.
 */
public abstract class ZookeeperWindowsProcessor implements Processor {

    @Override
    public void process(Context context) throws DubboTestException {
        ZookeeperWindowsContext zookeeperWindowsContext = (ZookeeperWindowsContext) context;
        this.doProcess(zookeeperWindowsContext);
    }

    /**
     * Use {@link Process} to handle the command.
     *
     * @param context    the global zookeeper context.
     * @throws DubboTestException when any exception occurred.
     */
    protected abstract void doProcess(ZookeeperWindowsContext context) throws DubboTestException;
}
