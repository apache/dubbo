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

public class MethodCallbackImpl implements MethodCallback {
    private String onInvoke;
    private String onReturn;
    private String onThrow;

    @Override
    public void oninvoke(String request) {
        this.onInvoke = "dubbo invoke success";
    }

    @Override
    public void onreturn(String response, String request) {
        this.onReturn = "dubbo return success";
    }

    @Override
    public void onthrow(Throwable ex, String request) {
        this.onThrow = "dubbo throw exception";
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
}
