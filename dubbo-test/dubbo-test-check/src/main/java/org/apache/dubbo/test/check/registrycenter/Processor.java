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

import org.apache.dubbo.test.check.exception.DubboTestException;

/**
 * Define the processor to execute {@link Process} with the {@link org.apache.dubbo.test.check.registrycenter.Initializer.Context}
 */
public interface Processor {

    /**
     * Process the command with the global context.
     *
     * @param context the global context.
     * @throws DubboTestException when any exception occurred.
     */
    void process(Context context) throws DubboTestException;
}
