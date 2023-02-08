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
package org.apache.dubbo.common.utils;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;

public class SerializeSecurityManager {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(SerializeSecurityManager.class);

    private final Set<String> allowedPrefix = new ConcurrentHashSet<>();

    private final Set<String> alwaysAllowedPrefix = new ConcurrentHashSet<>();

    private final Set<String> disAllowedPrefix = new ConcurrentHashSet<>();

    private final Set<AllowClassNotifyListener> listeners = new ConcurrentHashSet<>();

    private final Set<String> warnedClasses = new ConcurrentHashSet<>(1);

    private volatile SerializeCheckStatus checkStatus = null;

    private volatile Boolean checkSerializable = null;

    public void addToAlwaysAllowed(String className) {
        boolean modified = alwaysAllowedPrefix.add(className);

        if (modified) {
            notifyPrefix();
        }
    }

    public void addToAllowed(String className) {
        if (disAllowedPrefix.stream().anyMatch(className::startsWith)) {
            return;
        }

        boolean modified = allowedPrefix.add(className);

        if (modified) {
            notifyPrefix();
        }
    }

    public void addToDisAllowed(String className) {
        boolean modified = disAllowedPrefix.add(className);
        modified = allowedPrefix.removeIf(allow -> allow.startsWith(className)) || modified;

        if (modified) {
            notifyPrefix();
        }

        String lowerCase = className.toLowerCase(Locale.ROOT);
        if (!Objects.equals(lowerCase, className)) {
            addToDisAllowed(lowerCase);
        }
    }

    public void setCheckStatus(SerializeCheckStatus checkStatus) {
        if (this.checkStatus == null) {
            this.checkStatus = checkStatus;
            logger.info("Serialize check level: " + checkStatus.name());
            notifyCheckStatus();
            return;
        }

        // If has been set to WARN, ignore STRICT
        if (this.checkStatus.level() <= checkStatus.level()) {
            return;
        }

        this.checkStatus = checkStatus;
        logger.info("Serialize check level: " + checkStatus.name());
        notifyCheckStatus();
    }

    public void setCheckSerializable(boolean checkSerializable) {
        if (this.checkSerializable == null || (Boolean.TRUE.equals(this.checkSerializable) && !checkSerializable)) {
            this.checkSerializable = checkSerializable;
            logger.info("Serialize check serializable: " + checkSerializable);
            notifyCheckSerializable();
        }
    }

    public void registerListener(AllowClassNotifyListener listener) {
        listeners.add(listener);
        listener.notifyPrefix(getAllowedPrefix(), getDisAllowedPrefix());
        listener.notifyCheckSerializable(isCheckSerializable());
        listener.notifyCheckStatus(getCheckStatus());
    }

    private void notifyPrefix() {
        for (AllowClassNotifyListener listener : listeners) {
            listener.notifyPrefix(getAllowedPrefix(), getDisAllowedPrefix());
        }
    }

    private void notifyCheckStatus() {
        for (AllowClassNotifyListener listener : listeners) {
            listener.notifyCheckStatus(getCheckStatus());
        }
    }

    private void notifyCheckSerializable() {
        for (AllowClassNotifyListener listener : listeners) {
            listener.notifyCheckSerializable(isCheckSerializable());
        }
    }

    protected SerializeCheckStatus getCheckStatus() {
        return checkStatus == null ? AllowClassNotifyListener.DEFAULT_STATUS : checkStatus;
    }

    protected Set<String> getAllowedPrefix() {
        Set<String> set = new ConcurrentHashSet<>();
        set.addAll(allowedPrefix);
        set.addAll(alwaysAllowedPrefix);
        return set;
    }

    protected Set<String> getDisAllowedPrefix() {
        Set<String> set = new ConcurrentHashSet<>();
        set.addAll(disAllowedPrefix);
        return set;
    }

    protected boolean isCheckSerializable() {
        return checkSerializable == null || checkSerializable;
    }

    public Set<String> getWarnedClasses() {
        return warnedClasses;
    }
}
