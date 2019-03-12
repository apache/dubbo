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
package org.apache.dubbo.config.builders;

import org.apache.dubbo.config.ArgumentConfig;
import org.apache.dubbo.config.MethodConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a builder for build {@link MethodConfig}.
 *
 * @since 2.7
 */
public class MethodBuilder extends AbstractMethodBuilder<MethodConfig, MethodBuilder> {
    /**
     * The method name
     */
    private String name;

    /**
     * Stat
     */
    private Integer stat;

    /**
     * Whether to retry
     */
    private Boolean retry;

    /**
     * If it's reliable
     */
    private Boolean reliable;

    /**
     * Thread limits for method invocations
     */
    private Integer executes;

    /**
     * If it's deprecated
     */
    private Boolean deprecated;

    /**
     * Whether to enable sticky
     */
    private Boolean sticky;

    /**
     * Whether need to return
     */
    private Boolean isReturn;

    /**
     * Callback instance when async-call is invoked
     */
    private Object oninvoke;

    /**
     * Callback method when async-call is invoked
     */
    private String oninvokeMethod;

    /**
     * Callback instance when async-call is returned
     */
    private Object onreturn;

    /**
     * Callback method when async-call is returned
     */
    private String onreturnMethod;

    /**
     * Callback instance when async-call has exception thrown
     */
    private Object onthrow;

    /**
     * Callback method when async-call has exception thrown
     */
    private String onthrowMethod;

    /**
     * The method arguments
     */
    private List<ArgumentConfig> arguments;

    /**
     * These properties come from MethodConfig's parent Config module, they will neither be collected directly from xml or API nor be delivered to url
     */
    private String service;
    private String serviceId;

    public MethodBuilder name(String name) {
        this.name = name;
        return getThis();
    }

    public MethodBuilder stat(Integer stat) {
        this.stat = stat;
        return getThis();
    }

    public MethodBuilder retry(Boolean retry) {
        this.retry = retry;
        return getThis();
    }

    public MethodBuilder reliable(Boolean reliable) {
        this.reliable = reliable;
        return getThis();
    }

    public MethodBuilder executes(Integer executes) {
        this.executes = executes;
        return getThis();
    }

    public MethodBuilder deprecated(Boolean deprecated) {
        this.deprecated = deprecated;
        return getThis();
    }

    public MethodBuilder sticky(Boolean sticky) {
        this.sticky = sticky;
        return getThis();
    }

    public MethodBuilder isReturn(Boolean isReturn) {
        this.isReturn = isReturn;
        return getThis();
    }

    public MethodBuilder oninvoke(Object oninvoke) {
        this.oninvoke = oninvoke;
        return getThis();
    }

    public MethodBuilder oninvokeMethod(String oninvokeMethod) {
        this.oninvokeMethod = oninvokeMethod;
        return getThis();
    }

    public MethodBuilder onreturn(Object onreturn) {
        this.onreturn = onreturn;
        return getThis();
    }

    public MethodBuilder onreturnMethod(String onreturnMethod) {
        this.onreturnMethod = onreturnMethod;
        return getThis();
    }

    public MethodBuilder onthrow(Object onthrow) {
        this.onthrow = onthrow;
        return getThis();
    }

    public MethodBuilder onthrowMethod(String onthrowMethod) {
        this.onthrowMethod = onthrowMethod;
        return getThis();
    }

    public MethodBuilder addArguments(List<? extends ArgumentConfig> arguments) {
        if (this.arguments == null) {
            this.arguments = new ArrayList<>();
        }
        this.arguments.addAll(arguments);
        return getThis();
    }

    public MethodBuilder addArgument(ArgumentConfig argument) {
        if (this.arguments == null) {
            this.arguments = new ArrayList<>();
        }
        this.arguments.add(argument);
        return getThis();
    }

    public MethodBuilder service(String service) {
        this.service = service;
        return getThis();
    }

    public MethodBuilder serviceId(String serviceId) {
        this.serviceId = serviceId;
        return getThis();
    }

    public MethodConfig build() {
        MethodConfig methodConfig = new MethodConfig();
        super.build(methodConfig);

        methodConfig.setArguments(arguments);
        methodConfig.setDeprecated(deprecated);
        methodConfig.setExecutes(executes);
        methodConfig.setName(name);
        methodConfig.setOninvoke(oninvoke);
        methodConfig.setOninvokeMethod(oninvokeMethod);
        methodConfig.setOnreturn(onreturn);
        methodConfig.setOnreturnMethod(onreturnMethod);
        methodConfig.setOnthrow(onthrow);
        methodConfig.setOnthrowMethod(onthrowMethod);
        methodConfig.setReturn(isReturn);
        methodConfig.setService(service);
        methodConfig.setServiceId(serviceId);
        methodConfig.setSticky(sticky);
        methodConfig.setReliable(reliable);
        methodConfig.setStat(stat);
        methodConfig.setRetry(retry);

        return methodConfig;
    }

    @Override
    protected MethodBuilder getThis() {
        return this;
    }
}
