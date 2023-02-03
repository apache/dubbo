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
package org.apache.dubbo.qos.command.util;

import java.util.Collections;
import java.util.Set;

import org.apache.dubbo.common.utils.AllowClassNotifyListener;
import org.apache.dubbo.common.utils.SerializeCheckStatus;
import org.apache.dubbo.common.utils.SerializeSecurityManager;
import org.apache.dubbo.rpc.model.FrameworkModel;

public class SerializeCheckUtils implements AllowClassNotifyListener {
    private final SerializeSecurityManager manager;
    private volatile Set<String> allowedList = Collections.emptySet();
    private volatile Set<String> disAllowedList = Collections.emptySet();
    private volatile SerializeCheckStatus status = AllowClassNotifyListener.DEFAULT_STATUS;
    private volatile boolean checkSerializable = true;

    public SerializeCheckUtils(FrameworkModel frameworkModel) {
        manager = frameworkModel.getBeanFactory().getOrRegisterBean(SerializeSecurityManager.class);
        manager.registerListener(this);
    }

    @Override
    public void notifyPrefix(Set<String> allowedList, Set<String> disAllowedList) {
        this.allowedList = allowedList;
        this.disAllowedList = disAllowedList;
    }

    @Override
    public void notifyCheckStatus(SerializeCheckStatus status) {
        this.status = status;
    }

    @Override
    public void notifyCheckSerializable(boolean checkSerializable) {
        this.checkSerializable = checkSerializable;
    }

    public Set<String> getAllowedList() {
        return allowedList;
    }

    public Set<String> getDisAllowedList() {
        return disAllowedList;
    }

    public SerializeCheckStatus getStatus() {
        return status;
    }

    public boolean isCheckSerializable() {
        return checkSerializable;
    }

    public Set<String> getWarnedClasses() {
        return manager.getWarnedClasses();
    }
}
