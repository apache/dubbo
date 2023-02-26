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
package org.apache.dubbo.rpc.cluster.directory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.dubbo.rpc.AttachmentsAdapter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.model.ServiceModel;

import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_VERSION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.rpc.Constants.TOKEN_KEY;

/**
 * MockInvocation.java
 */
class MockDirInvocation implements Invocation {

    private Map<String, Object> attachments;

    public MockDirInvocation() {
        attachments = new HashMap<>();
        attachments.put(PATH_KEY, "dubbo");
        attachments.put(GROUP_KEY, "dubbo");
        attachments.put(VERSION_KEY, "1.0.0");
        attachments.put(DUBBO_VERSION_KEY, "1.0.0");
        attachments.put(TOKEN_KEY, "sfag");
        attachments.put(TIMEOUT_KEY, "1000");
    }

    @Override
    public String getTargetServiceUniqueName() {
        return null;
    }

    @Override
    public String getProtocolServiceKey() {
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
        return new Object[]{"aa"};
    }

    public Map<String, String> getAttachments() {
        return new AttachmentsAdapter.ObjectToStringMap(attachments);
    }

    @Override
    public Map<String, Object> getObjectAttachments() {
        return attachments;
    }

    @Override
    public Map<String, Object> copyObjectAttachments() {
        return new HashMap<>(attachments);
    }

    @Override
    public void foreachAttachment(Consumer<Map.Entry<String, Object>> consumer) {
        attachments.entrySet().forEach(consumer);
    }

    @Override
    public void setAttachment(String key, String value) {
        setObjectAttachment(key, value);
    }

    @Override
    public void setAttachment(String key, Object value) {
       setObjectAttachment(key, value);
    }

    @Override
    public void setObjectAttachment(String key, Object value) {
        attachments.put(key, value);
    }

    @Override
    public void setAttachmentIfAbsent(String key, String value) {
        setObjectAttachmentIfAbsent(key, value);
    }

    @Override
    public void setAttachmentIfAbsent(String key, Object value) {
        setObjectAttachmentIfAbsent(key, value);
    }

    @Override
    public void setObjectAttachmentIfAbsent(String key, Object value) {
        attachments.putIfAbsent(key, value);
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
    public void setServiceModel(ServiceModel serviceModel) {

    }

    @Override
    public ServiceModel getServiceModel() {
        return null;
    }

    @Override
    public Map<Object, Object> getAttributes() {
        return null;
    }

    public String getAttachment(String key) {
        return (String) getObjectAttachment(key);
    }

    @Override
    public Object getObjectAttachment(String key) {
        return attachments.get(key);
    }

    public String getAttachment(String key, String defaultValue) {
        return (String) getObjectAttachment(key, defaultValue);
    }

    @Override
    public void addInvokedInvoker(Invoker<?> invoker) {

    }

    @Override
    public List<Invoker<?>> getInvokedInvokers() {
        return null;
    }

    @Override
    public Object getObjectAttachment(String key, Object defaultValue) {
        Object result = attachments.get(key);
        if (result == null) {
            return defaultValue;
        }
        return result;
    }

}
