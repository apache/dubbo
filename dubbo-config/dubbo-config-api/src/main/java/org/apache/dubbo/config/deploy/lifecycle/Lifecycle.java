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
package org.apache.dubbo.config.deploy.lifecycle;

import org.apache.dubbo.config.deploy.context.ModelContext;
import org.apache.dubbo.rpc.model.ScopeModel;

/**
 * Lifecycle. The abstraction of dubbo model lifecycle operations.
 */
public interface Lifecycle<M extends ScopeModel,T extends ModelContext<M>>{

    /**
     * If this lifecycle need to initialize.
     */
    boolean needInitialize(T context);

    /**
     * @see ModelContext#runStart()
     */
    default void start(T context){}

    /**
     * @see ModelContext#runInitialize()
     */
    default void initialize(T context){}

    /**
     * @see ModelContext#runPreDestroy()
     */
    default void preDestroy(T context) {}

    /**
     * @see ModelContext#runPostDestroy()
     */
    default void postDestroy(T context) {}
}
