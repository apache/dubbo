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

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages serialization security by maintaining allowlists and disallow lists,
 * along with listeners to notify changes in security settings.
 */
public class SerializeSecurityManager {

    private static final ErrorTypeAwareLogger logger =
            LoggerFactory.getErrorTypeAwareLogger(SerializeSecurityManager.class);

    private final Set<String> allowedPrefix = ConcurrentHashMap.newKeySet();
    private final Set<String> alwaysAllowedPrefix = ConcurrentHashMap.newKeySet();
    private final Set<String> disAllowedPrefix = ConcurrentHashMap.newKeySet();
    private final Set<AllowClassNotifyListener> listeners = ConcurrentHashMap.newKeySet();
    private final Set<String> warnedClasses = ConcurrentHashMap.newKeySet();

    private volatile SerializeCheckStatus checkStatus = null;
    private volatile SerializeCheckStatus defaultCheckStatus = AllowClassNotifyListener.DEFAULT_STATUS;
    private volatile Boolean checkSerializable = null;

    /**
     * Adds a class name to the always-allowed list.
     *
     * @param className The class name to allow.
     */
    public void addToAlwaysAllowed(String className) {
        if (alwaysAllowedPrefix.add(className)) {
            notifyPrefix();
        }
    }

    /**
     * Adds a class name to the allowed list, unless it's already in the disallowed list.
     *
     * @param className The class name to allow.
     */
    public void addToAllowed(String className) {
        if (disAllowedPrefix.stream().anyMatch(className::startsWith)) {
            return;
        }

        if (allowedPrefix.add(className)) {
            notifyPrefix();
        }
    }

    /**
     * Adds a class name to the disallowed list, removing any conflicting allowed entries.
     *
     * @param className The class name to disallow.
     */
    public void addToDisAllowed(String className) {
        boolean modified = disAllowedPrefix.add(className);
        modified = allowedPrefix.removeIf(allow -> allow.startsWith(className)) || modified;

        if (modified) {
            notifyPrefix();
        }

        // Ensure lowercase variant is also disallowed
        String lowerCase = className.toLowerCase(Locale.ROOT);
        if (!Objects.equals(lowerCase, className)) {
            addToDisAllowed(lowerCase);
        }
    }

    /**
     * Sets the serialization security check status.
     *
     * @param checkStatus The new check status.
     */
    public void setCheckStatus(SerializeCheckStatus checkStatus) {
        if (this.checkStatus == null) {
            this.checkStatus = checkStatus;
            logger.info("Serialize check level: " + checkStatus.name());
            notifyCheckStatus();
            return;
        }

        // If the current status is WARN, ignore STRICT updates
        if (this.checkStatus == SerializeCheckStatus.WARN && checkStatus == SerializeCheckStatus.STRICT) {
            return;
        }

        this.checkStatus = checkStatus;
        logger.info("Serialize check level updated to: " + checkStatus.name());
        notifyCheckStatus();
    }

    /**
     * Sets the default serialization security check status.
     *
     * @param checkStatus The default check status.
     */
    public void setDefaultCheckStatus(SerializeCheckStatus checkStatus) {
        this.defaultCheckStatus = checkStatus;
        logger.info("Serialize check default level set to: " + checkStatus.name());
        notifyCheckStatus();
    }

    /**
     * Enables or disables the requirement for serialized classes to implement {@code Serializable}.
     *
     * @param checkSerializable {@code true} to enable, {@code false} to disable.
     */
    public void setCheckSerializable(boolean checkSerializable) {
        if (this.checkSerializable == null || (Boolean.TRUE.equals(this.checkSerializable) && !checkSerializable)) {
            this.checkSerializable = checkSerializable;
            logger.info("Serialize check serializable set to: " + checkSerializable);
            notifyCheckSerializable();
        }
    }

    /**
     * Registers a listener to receive updates when security settings change.
     *
     * @param listener The listener to register.
     */
    public void registerListener(AllowClassNotifyListener listener) {
        listeners.add(listener);
        listener.notifyPrefix(getAllowedPrefix(), getDisAllowedPrefix());
        listener.notifyCheckSerializable(isCheckSerializable());
        listener.notifyCheckStatus(getCheckStatus());
    }

    /**
     * Notifies listeners about prefix updates.
     */
    private void notifyPrefix() {
        for (AllowClassNotifyListener listener : listeners) {
            listener.notifyPrefix(getAllowedPrefix(), getDisAllowedPrefix());
        }
    }

    /**
     * Notifies listeners about check status updates.
     */
    private void notifyCheckStatus() {
        for (AllowClassNotifyListener listener : listeners) {
            listener.notifyCheckStatus(getCheckStatus());
        }
    }

    /**
     * Notifies listeners about changes in the serializable check requirement.
     */
    private void notifyCheckSerializable() {
        for (AllowClassNotifyListener listener : listeners) {
            listener.notifyCheckSerializable(isCheckSerializable());
        }
    }

    /**
     * Retrieves the current check status, falling back to the default if not set.
     *
     * @return The serialization check status.
     */
    protected SerializeCheckStatus getCheckStatus() {
        return checkStatus == null ? defaultCheckStatus : checkStatus;
    }

    /**
     * Retrieves the set of allowed class name prefixes.
     *
     * @return The set of allowed prefixes.
     */
    protected Set<String> getAllowedPrefix() {
        Set<String> set = ConcurrentHashMap.newKeySet();
        set.addAll(allowedPrefix);
        set.addAll(alwaysAllowedPrefix);
        return set;
    }

    /**
     * Retrieves the set of disallowed class name prefixes.
     *
     * @return The set of disallowed prefixes.
     */
    protected Set<String> getDisAllowedPrefix() {
        return ConcurrentHashMap.newKeySet(disAllowedPrefix);
    }

    /**
     * Determines if the serializable check is enabled.
     *
     * @return {@code true} if enabled, {@code false} otherwise.
     */
    protected boolean isCheckSerializable() {
        return checkSerializable == null || checkSerializable;
    }

    /**
     * Retrieves the set of warned classes.
     *
     * @return The set of warned class names.
     */
    public Set<String> getWarnedClasses() {
        return warnedClasses;
    }
}
