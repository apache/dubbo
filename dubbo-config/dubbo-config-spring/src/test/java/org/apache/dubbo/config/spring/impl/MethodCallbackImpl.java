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
import org.apache.dubbo.rpc.RpcContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;

public class MethodCallbackImpl implements MethodCallback {
    public static AtomicInteger cnt = new AtomicInteger();
    public static AtomicInteger cnt1 = new AtomicInteger();
    public static AtomicInteger cnt2 = new AtomicInteger();

    private String onInvoke1;
    private String onReturn1;
    private String onThrow1;
    
    private String onInvoke2;
    private String onReturn2;
    private String onThrow2;

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
            // Do not use RpcContext.getAsyncConsumerUrl() because it's not set yet
            switch (RpcContext.getContext().getConsumerUrl().getParameter("refId")) {
            case "ref-1":
                this.onInvoke1 = "dubbo invoke success";
                break;
            case "ref-2":
                this.onInvoke2 = "dubbo invoke success(2)";
                break;
            }
        } catch (Exception e) {
            switch (RpcContext.getContext().getConsumerUrl().getParameter("refId")) {
            case "ref-1":
                this.onInvoke1 = e.toString();
                break;
            case "ref-2":
                this.onInvoke2 = e.toString();
                break;
            }
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onreturn(String response, String request) {
        try {
            checkInjection();
            checkTranscation();
            // Do not use RpcContext.getContext().getConsumerUrl() because it might be reused by other async callings
            switch (RpcContext.getAsyncConsumerUrl().getParameter("refId")) {
            case "ref-1":
                this.onReturn1 = "dubbo return success";
                if (cnt1.incrementAndGet() == 2) {
                    this.onReturn1 = "double 1!";
                }
                break;
            case "ref-2":
                this.onReturn2 = "dubbo return success(2)";
                if (cnt2.incrementAndGet() == 2) {
                    this.onReturn2 = "double 2!";
                }
                break;
            }
        } catch (Exception e) {
            switch (RpcContext.getAsyncConsumerUrl().getParameter("refId")) {
            case "ref-1":
                this.onReturn1 = e.toString();
                break;
            case "ref-2":
                this.onReturn2 = e.toString();
                break;
            }
            throw e;
        } finally {
            cnt.incrementAndGet();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onthrow(Throwable ex, String request) {
        try {
            checkInjection();
            checkTranscation();
            // Do not use RpcContext.getContext().getConsumerUrl() because it might be reused by other async callings
            switch (RpcContext.getAsyncConsumerUrl().getParameter("refId")) {
            case "ref-1":
                this.onThrow1 = "dubbo throw exception";
                break;
            case "ref-2":
                this.onThrow2 = "dubbo throw exception(2)";
                break;
            }
        } catch (Exception e) {
            switch (RpcContext.getAsyncConsumerUrl().getParameter("refId")) {
            case "ref-1":
                this.onThrow1 = e.toString();
                break;
            case "ref-2":
                this.onThrow2 = e.toString();
                break;
            }
            throw e;
        }
    }

    public String getOnInvoke() {
        return this.onInvoke1 + ',' + this.onInvoke2;
    }

    public String getOnReturn() {
        return this.onReturn1 + ',' + this.onReturn2;
    }

    public String getOnThrow() {
        return this.onThrow1 + ',' + this.onThrow2;
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