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
package org.apache.dubbo.common;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Parameters for backward compatibility for version prior to 2.0.5
 *
 * @deprecated
 */
@Deprecated
public class Parameters {
    protected static final Logger logger = LoggerFactory.getLogger(Parameters.class);
    private final Map<String, String> parameters;

    public Parameters(String... pairs) {
        this(toMap(pairs));
    }

    public Parameters(Map<String, String> parameters) {
        this.parameters = Collections.unmodifiableMap(parameters != null ? new HashMap<>(parameters) : new HashMap<>(0));
    }

    private static Map<String, String> toMap(String... pairs) {
        return CollectionUtils.toStringMap(pairs);
    }

    public static Parameters parseParameters(String query) {
        return new Parameters(StringUtils.parseQueryString(query));
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public <T> T getExtension(Class<T> type, String key) {
        String name = getParameter(key);
        return ExtensionLoader.getExtensionLoader(type).getExtension(name);
    }

    public <T> T getExtension(Class<T> type, String key, String defaultValue) {
        String name = getParameter(key, defaultValue);
        return ExtensionLoader.getExtensionLoader(type).getExtension(name);
    }

    public <T> T getMethodExtension(Class<T> type, String method, String key) {
        String name = getMethodParameter(method, key);
        return ExtensionLoader.getExtensionLoader(type).getExtension(name);
    }

    public <T> T getMethodExtension(Class<T> type, String method, String key, String defaultValue) {
        String name = getMethodParameter(method, key, defaultValue);
        return ExtensionLoader.getExtensionLoader(type).getExtension(name);
    }

    public String getDecodedParameter(String key) {
        return getDecodedParameter(key, null);
    }

    public String getDecodedParameter(String key, String defaultValue) {
        String value = getParameter(key, defaultValue);
        if (value != null && value.length() > 0) {
            try {
                value = URLDecoder.decode(value, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return value;
    }

    public String getParameter(String key) {
        String value = parameters.get(key);
        if (value == null || value.length() == 0) {
            value = parameters.get(Constants.HIDE_KEY_PREFIX + key);
        }
        if (value == null || value.length() == 0) {
            value = parameters.get(Constants.DEFAULT_KEY_PREFIX + key);
        }
        if (value == null || value.length() == 0) {
            value = parameters.get(Constants.HIDE_KEY_PREFIX + Constants.DEFAULT_KEY_PREFIX + key);
        }
        return value;
    }

    public String getParameter(String key, String defaultValue) {
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        return value;
    }

    public int getIntParameter(String key) {
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return 0;
        }
        return Integer.parseInt(value);
    }

    public int getIntParameter(String key, int defaultValue) {
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    public int getPositiveIntParameter(String key, int defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        int i = Integer.parseInt(value);
        if (i > 0) {
            return i;
        }
        return defaultValue;
    }

    public boolean getBooleanParameter(String key) {
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return false;
        }
        return Boolean.parseBoolean(value);
    }

    public boolean getBooleanParameter(String key, boolean defaultValue) {
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    public boolean hasParamter(String key) {
        String value = getParameter(key);
        return value != null && value.length() > 0;
    }

    public String getMethodParameter(String method, String key) {
        String value = parameters.get(method + "." + key);
        if (value == null || value.length() == 0) {
            value = parameters.get(Constants.HIDE_KEY_PREFIX + method + "." + key);
        }
        if (value == null || value.length() == 0) {
            return getParameter(key);
        }
        return value;
    }

    public String getMethodParameter(String method, String key, String defaultValue) {
        String value = getMethodParameter(method, key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        return value;
    }

    public int getMethodIntParameter(String method, String key) {
        String value = getMethodParameter(method, key);
        if (value == null || value.length() == 0) {
            return 0;
        }
        return Integer.parseInt(value);
    }

    public int getMethodIntParameter(String method, String key, int defaultValue) {
        String value = getMethodParameter(method, key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    public int getMethodPositiveIntParameter(String method, String key, int defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        String value = getMethodParameter(method, key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        int i = Integer.parseInt(value);
        if (i > 0) {
            return i;
        }
        return defaultValue;
    }

    public boolean getMethodBooleanParameter(String method, String key) {
        String value = getMethodParameter(method, key);
        if (value == null || value.length() == 0) {
            return false;
        }
        return Boolean.parseBoolean(value);
    }

    public boolean getMethodBooleanParameter(String method, String key, boolean defaultValue) {
        String value = getMethodParameter(method, key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    public boolean hasMethodParamter(String method, String key) {
        String value = getMethodParameter(method, key);
        return value != null && value.length() > 0;
    }

    @Override
    public boolean equals(Object o) {
        return parameters.equals(o);
    }

    @Override
    public int hashCode() {
        return parameters.hashCode();
    }

    @Override
    public String toString() {
        return StringUtils.toQueryString(getParameters());
    }

}
