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
package org.apache.dubbo.rpc.cluster.router.condition.matcher;

import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.rpc.model.ModuleModel;

/**
 * Factory of ConditionMatcher instances.
 */
@SPI
public interface ConditionMatcherFactory {
    /**
     * Check if the key is of the form of the current matcher type which this factory instance represents..
     *
     * @param key the key of a particular form
     * @return true if matches, otherwise false
     */
    boolean shouldMatch(String key);

    /**
     * Create a matcher instance for the key.
     *
     * @param key   the key value conforms to a specific matcher specification
     * @param model module model
     * @return the specific matcher instance
     */
    ConditionMatcher createMatcher(String key, ModuleModel model);
}
