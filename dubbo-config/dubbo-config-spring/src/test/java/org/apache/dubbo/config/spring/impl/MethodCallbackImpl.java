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
    private String onInvoke1;
    private String onReturn1;
    private String onThrow1;

    private String onInvoke2;
    private String onReturn2;
    private String onThrow2;

    private AtomicInteger cnt = new AtomicInteger();

    @Autowired
    private Environment environment;

    @Autowired
    private ApplicationContext context;

    @PostConstruct
    protected void init() {
        checkInjection();
    }

    @Override
    public int getCnt() {
        return cnt.get();
    }

    @Override
    public void reset() {
        cnt.set(0);
        onInvoke1 = "";
        onInvoke2 = "";
        onReturn1 = "";
        onReturn2 = "";
        onThrow1 = "";
        onThrow2 = "";
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void oninvoke(String request) {
        try {
            checkInjection();
            checkTranscation();
            // use getConsumerUrl() at oninvoke: because the correct asyncConsumerUrl is not set yet.
            switch (RpcContext.getContext().getConsumerUrl().getParameter("refId")) {
            case "ref-1":
                synchronized (onInvoke1) { 
                    onInvoke1 += "dubbo invoke success";
                }
                break;
            case "ref-2":
                synchronized (onInvoke2) {
                    onInvoke2 += "dubbo invoke success(2)";
                }
                break;
            }
        } catch (Exception e) {
            switch (RpcContext.getContext().getConsumerUrl().getParameter("refId")) {
            case "ref-1":
                synchronized (onInvoke1) {
                    onInvoke1 += e.toString();
                }
                break;
            case "ref-2":
                synchronized (onInvoke2) {
                    onInvoke2 += e.toString();
                }
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
            // use getAsyncConsumerUrl() at onreturn: because the consumerUrl will be overridden by the following invocations of the same thread.
            switch (RpcContext.getAsyncConsumerUrl().getParameter("refId")) {
            case "ref-1":
                synchronized (onReturn1) {
                    onReturn1 += "dubbo return success";
                }
                break;
            case "ref-2":
                synchronized (onReturn2) {
                    onReturn2 += "dubbo return success(2)";
                }
                break;
            }
        } catch (Exception e) {
            switch (RpcContext.getAsyncConsumerUrl().getParameter("refId")) {
            case "ref-1":
                synchronized (onReturn1) {
                    onReturn1 += e.toString();
                }
                break;
            case "ref-2":
                synchronized (onReturn2) {
                    onReturn2 += e.toString();
                }
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
            // use getAsyncConsumerUrl() at onthrow: because the consumerUrl will be overridden by the following invocations of the same thread.
            switch (RpcContext.getAsyncConsumerUrl().getParameter("refId")) {
            case "ref-1":
                synchronized (onThrow1) {
                    onThrow1 += "dubbo throw exception";
                }
                break;
            case "ref-2":
                synchronized (onThrow2) {
                    onThrow2 += "dubbo throw exception(2)";
                }
                break;
            }
        } catch (Exception e) {
            switch (RpcContext.getAsyncConsumerUrl().getParameter("refId")) {
            case "ref-1":
                synchronized (onThrow1) {
                    onThrow1 += e.toString();
                }
                break;
            case "ref-2":
                synchronized (onThrow2) {
                    onThrow2 += e.toString();
                }
                break;
            }
            throw e;
        }
    }

    public String getOnInvoke() {
        return onInvoke1 + ',' + onInvoke2;
    }

    public String getOnReturn() {
        return onReturn1 + ',' + onReturn2;
    }

    public String getOnThrow() {
        return onThrow1 + ',' + onThrow2;
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