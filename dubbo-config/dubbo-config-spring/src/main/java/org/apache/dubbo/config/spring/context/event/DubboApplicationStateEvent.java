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
package org.apache.dubbo.config.spring.context.event;

import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.springframework.context.ApplicationEvent;

/**
 * Dubbo's application state event on starting/started/stopping/stopped
 */
public class DubboApplicationStateEvent extends ApplicationEvent {

    private final DeployState state;

    private Throwable cause;

    public DubboApplicationStateEvent(ApplicationModel applicationModel, DeployState state) {
        super(applicationModel);
        this.state = state;
    }

    public DubboApplicationStateEvent(ApplicationModel applicationModel, DeployState state, Throwable cause) {
        super(applicationModel);
        this.state = state;
        this.cause = cause;
    }

    public ApplicationModel getApplicationModel() {
        return (ApplicationModel) getSource();
    }

    public DeployState getState() {
        return state;
    }

    public Throwable getCause() {
        return cause;
    }
}
