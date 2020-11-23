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
package org.apache.dubbo.registry.nacos;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.StringUtils;

import java.util.Arrays;
import java.util.Objects;

import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.DEFAULT_CATEGORY;
import static org.apache.dubbo.common.utils.StringUtils.isBlank;

/**
 * The service name of Nacos
 *
 * @since 2.7.3
 */
public class NacosServiceName {

    public static final String NAME_SEPARATOR = ":";

    public static final String VALUE_SEPARATOR = ",";

    public static final String WILDCARD = "*";

    public static final String DEFAULT_PARAM_VALUE = "";

    private static final int CATEGORY_INDEX = 0;

    private static final int SERVICE_INTERFACE_INDEX = 1;

    private static final int SERVICE_VERSION_INDEX = 2;

    private static final int SERVICE_GROUP_INDEX = 3;

    private String category;

    private String serviceInterface;

    private String version;

    private String group;

    private String value;

    public NacosServiceName() {
    }

    public NacosServiceName(URL url) {
        serviceInterface = url.getParameter(INTERFACE_KEY);
        category = isConcrete(serviceInterface) ? DEFAULT_CATEGORY : url.getCategory();
        version = url.getVersion(DEFAULT_PARAM_VALUE);
        group = url.getGroup(DEFAULT_PARAM_VALUE);
        value = toValue();
    }

    public NacosServiceName(String value) {
        this.value = value;
        String[] segments = value.split(NAME_SEPARATOR, -1);
        this.category = segments[CATEGORY_INDEX];
        this.serviceInterface = segments[SERVICE_INTERFACE_INDEX];
        this.version = segments[SERVICE_VERSION_INDEX];
        this.group = segments[SERVICE_GROUP_INDEX];
    }

    /**
     * Build an instance of {@link NacosServiceName}
     *
     * @param url
     * @return
     */
    public static NacosServiceName valueOf(URL url) {
        return new NacosServiceName(url);
    }

    /**
     * Is the concrete service name or not
     *
     * @return if concrete , return <code>true</code>, or <code>false</code>
     */
    public boolean isConcrete() {
        return isConcrete(serviceInterface) && isConcrete(version) && isConcrete(group);
    }

    public boolean isCompatible(NacosServiceName concreteServiceName) {

        if (!concreteServiceName.isConcrete()) { // The argument must be the concrete NacosServiceName
            return false;
        }

        // Not match comparison
        if (!StringUtils.isEquals(this.category, concreteServiceName.category)
                && !matchRange(this.category, concreteServiceName.category)) {
            return false;
        }

        if (!StringUtils.isEquals(this.serviceInterface, concreteServiceName.serviceInterface)) {
            return false;
        }

        // wildcard condition
        if (isWildcard(this.version)) {
            return true;
        }

        if (isWildcard(this.group)) {
            return true;
        }

        // range condition
        if (!StringUtils.isEquals(this.version, concreteServiceName.version)
                && !matchRange(this.version, concreteServiceName.version)) {
            return false;
        }

        if (!StringUtils.isEquals(this.group, concreteServiceName.group) &&
                !matchRange(this.group, concreteServiceName.group)) {
            return false;
        }

        return true;
    }

    private boolean matchRange(String range, String value) {
        if (isBlank(range)) {
            return true;
        }
        if (!isRange(range)) {
            return false;
        }
        String[] values = range.split(VALUE_SEPARATOR);
        return Arrays.asList(values).contains(value);
    }

    private boolean isConcrete(String value) {
        return !isWildcard(value) && !isRange(value);
    }

    private boolean isWildcard(String value) {
        return WILDCARD.equals(value);
    }

    private boolean isRange(String value) {
        return value != null && value.indexOf(VALUE_SEPARATOR) > -1 && value.split(VALUE_SEPARATOR).length > 1;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getServiceInterface() {
        return serviceInterface;
    }

    public void setServiceInterface(String serviceInterface) {
        this.serviceInterface = serviceInterface;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getValue() {
        if (value == null) {
            value = toValue();
        }
        return value;
    }

    private String toValue() {
        return new StringBuilder(category)
                .append(NAME_SEPARATOR).append(serviceInterface)
                .append(NAME_SEPARATOR).append(version)
                .append(NAME_SEPARATOR).append(group)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NacosServiceName)) {
            return false;
        }
        NacosServiceName that = (NacosServiceName) o;
        return Objects.equals(getValue(), that.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getValue());
    }

    @Override
    public String toString() {
        return getValue();
    }
}
