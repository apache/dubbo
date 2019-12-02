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
package org.apache.dubbo.service;

import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;

import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_VERSION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.rpc.Constants.TOKEN_KEY;

/**
 * MockInvocation.java
 */
public class MockInvocation implements Invocation {

    private String arg0;

    public MockInvocation(String arg0) {
        this.arg0 = arg0;
    }

    @Override
    public String getTargetServiceUniqueName() {
        return null;
    }

    public String getMethodName() {
        return "echo";
    }

    @Override
    public String getServiceName() {
        return "DemoService";
    }

    public Class<?>[] getParameterTypes() {
        return new Class[]{String.class};
    }

    public Object[] getArguments() {
        return new Object[]{arg0};
    }

    public Map<String, Object> getAttachments() {
        Map<String, Object> attachments = new HashMap<String, Object>();
        attachments.put(PATH_KEY, "dubbo");
        attachments.put(GROUP_KEY, "dubbo");
        attachments.put(VERSION_KEY, "1.0.0");
        attachments.put(DUBBO_VERSION_KEY, "1.0.0");
        attachments.put(TOKEN_KEY, "sfag");
        attachments.put(TIMEOUT_KEY, "1000");
        return attachments;
    }

    @Override
    public void setAttachment(String key, Object value) {

    }

    @Override
    public void setAttachmentIfAbsent(String key, Object value) {

    }

    public Invoker<?> getInvoker() {
        return null;
    }

    @Override
    public Object put(Object key, Object value) {
        return null;
    }

    @Override
    public Object get(Object key) {
        return null;
    }

    @Override
    public Map<Object, Object> getAttributes() {
        return null;
    }

    public Object getAttachment(String key) {
        return getAttachments().get(key);
    }

    public Object getAttachment(String key, Object defaultValue) {
        return getAttachments().get(key);
    }

}
