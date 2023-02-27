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
<<<<<<< HEAD
=======

>>>>>>> origin/3.2
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.PostConstruct;
<<<<<<< HEAD

public class MethodCallbackImpl implements MethodCallback {
    private String onInvoke;
    private String onReturn;
    private String onThrow;
=======
import java.util.concurrent.atomic.AtomicInteger;

public class MethodCallbackImpl implements MethodCallback {
    private String onInvoke1 = "";

    private final Object lock = new Object();

    private String onReturn1 = "";
    private String onThrow1 = "";

    private String onInvoke2 = "";
    private String onReturn2 = "";
    private String onThrow2 = "";
>>>>>>> origin/3.2

    @Autowired
    private Environment environment;

    @Autowired
    private ApplicationContext context;

<<<<<<< HEAD
=======
    public static AtomicInteger cnt = new AtomicInteger();

>>>>>>> origin/3.2
    @PostConstruct
    protected void init() {
        checkInjection();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
<<<<<<< HEAD
    public void oninvoke(String request) {
        try {
            checkInjection();
            checkTranscation();
            this.onInvoke = "dubbo invoke success";
        } catch (Exception e) {
            this.onInvoke = e.toString();
=======
    public void oninvoke1(String request) {
        try {
            checkInjection();
            checkTranscation();
            synchronized (lock) {
                this.onInvoke1 += "dubbo invoke success!";
            }
        } catch (Exception e) {
            synchronized (lock) {
                this.onInvoke1 += e.toString();
            }
            throw e;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void oninvoke2(String request) {
        try {
            checkInjection();
            checkTranscation();
            synchronized (lock) {
                this.onInvoke2 += "dubbo invoke success(2)!";
            }
        } catch (Exception e) {
            synchronized (lock) {
                this.onInvoke2 += e.toString();
            }
>>>>>>> origin/3.2
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
<<<<<<< HEAD
    public void onreturn(String response, String request) {
        try {
            checkInjection();
            checkTranscation();
            this.onReturn = "dubbo return success";
        } catch (Exception e) {
            this.onReturn = e.toString();
            throw e;
=======
    public void onreturn1(String response, String request) {
        try {
            checkInjection();
            checkTranscation();
            synchronized (lock) {
                this.onReturn1 += "dubbo return success!";
            }
        } catch (Exception e) {
            synchronized (lock) {
                this.onReturn1 += e.toString();
            }
            throw e;
        } finally {
            cnt.incrementAndGet();
>>>>>>> origin/3.2
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
<<<<<<< HEAD
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
=======
    public void onreturn2(String response, String request) {
        try {
            checkInjection();
            checkTranscation();
            synchronized (lock) {
                this.onReturn2 += "dubbo return success(2)!";
            }
        } catch (Exception e) {
            synchronized (lock) {
                this.onReturn2 += e.toString();
            }
            throw e;
        } finally {
            cnt.incrementAndGet();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onthrow1(Throwable ex, String request) {
        try {
            checkInjection();
            checkTranscation();
            synchronized (lock) {
                this.onThrow1 += "dubbo throw exception!";
            }
        } catch (Exception e) {
            synchronized (lock) {
                this.onThrow1 += e.toString();
            }
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onthrow2(Throwable ex, String request) {
        try {
            checkInjection();
            checkTranscation();
            synchronized (lock) {
                this.onThrow2 += "dubbo throw exception(2)!";
            }
        } catch (Exception e) {
            synchronized (lock) {
                this.onThrow2 += e.toString();
            }
            throw e;
        }
    }

    public String getOnInvoke1() {
        return this.onInvoke1;
    }

    public String getOnReturn1() {
        return this.onReturn1;
    }

    public String getOnThrow1() {
        return this.onThrow1;
    }

    public String getOnInvoke2() {
        return this.onInvoke2;
    }

    public String getOnReturn2() {
        return this.onReturn2;
    }

    public String getOnThrow2() {
        return this.onThrow2;
>>>>>>> origin/3.2
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
