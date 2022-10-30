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
package org.apache.dubbo.config.spring.impl;

import org.apache.dubbo.config.spring.api.MethodCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.PostConstruct;

public class MethodCallbackImpl implements MethodCallback {
    private String onInvoke;
    private String onReturn;
    private String onThrow;

    @Autowired
    private Environment environment;

    @Autowired
    private ApplicationContext context;

    @PostConstruct
    protected void init() {
        checkInjection();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void oninvoke(String request) {
        try {
            checkInjection();
            checkTranscation();
            this.onInvoke = "dubbo invoke success";
        } catch (Exception e) {
            this.onInvoke = e.toString();
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onreturn(String response, String request) {
        try {
            checkInjection();
            checkTranscation();
            this.onReturn = "dubbo return success";
        } catch (Exception e) {
            this.onReturn = e.toString();
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onthrow(Throwable ex, String request) {
        try {
            checkInjection();
            checkTranscation();
            this.onThrow = "dubbo throw exception";
        } catch (Exception e) {
            this.onThrow = e.toString();
            throw e;
        }
    }

    public String getOnInvoke() {
        return this.onInvoke;
    }

    public String getOnReturn() {
        return this.onReturn;
    }

    public String getOnThrow() {
        return this.onThrow;
    }

    private void checkInjection() {
        if (environment == null) {
            throw new IllegalStateException("environment is null");
        }
        if (context == null) {
            throw new IllegalStateException("application context is null");
        }
    }

    private void checkTranscation() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("No active transaction");
        }
    }

}
