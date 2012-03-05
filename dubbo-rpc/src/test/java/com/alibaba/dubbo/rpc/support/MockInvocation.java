/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.support;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;

/**
 * MockInvocation.java
 * 
 * @author tony.chenl
 */
public class MockInvocation implements Invocation {

    public String getMethodName() {
        return "echo";
    }

    public Class<?>[] getParameterTypes() {
        return new Class[] { String.class };
    }

    public Object[] getArguments() {
        return new Object[] { "aa" };
    }

    public Map<String, String> getAttachments() {
        Map<String, String> attachments = new HashMap<String, String>();
        attachments.put(Constants.PATH_KEY, "dubbo");
        attachments.put(Constants.GROUP_KEY, "dubbo");
        attachments.put(Constants.VERSION_KEY, "1.0.0");
        attachments.put(Constants.DUBBO_VERSION_KEY, "1.0.0");
        attachments.put(Constants.TOKEN_KEY, "sfag");
        attachments.put(Constants.TIMEOUT_KEY, "1000");
        return attachments;
    }

    public Invoker<?> getInvoker() {
        return null;
    }

    public String getAttachment(String key) {
        return getAttachments().get(key);
    }

    public String getAttachment(String key, String defaultValue) {
        return getAttachments().get(key);
    }

}