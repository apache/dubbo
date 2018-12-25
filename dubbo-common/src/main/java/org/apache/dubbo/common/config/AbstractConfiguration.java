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
package org.apache.dubbo.common.config;

public abstract class AbstractConfiguration implements Configuration {

    @Override
    public String getString(String key) {
        return convert(String.class, key, null);
    }

    @Override
    public String getString(String key, String defaultValue) {
        return convert(String.class, key, defaultValue);
    }

    @Override
    public final Object getProperty(String key) {
        return getProperty(key, null);
    }

    @Override
    public Object getProperty(String key, Object defaultValue) {
        Object value = getInternalProperty(key);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    @Override
    public boolean containsKey(String key) {
        return getProperty(key) != null;
    }

    protected abstract Object getInternalProperty(String key);

    private <T> T convert(Class<T> cls, String key, T defaultValue) {
        // we only process String properties for now
        String value = (String) getProperty(key);

        if (value == null) {
            return defaultValue;
        }

        Object obj = value;
        if (cls.isInstance(value)) {
            return cls.cast(value);
        }

        if (String.class.equals(cls)) {
            return cls.cast(value);
        }

        if (Boolean.class.equals(cls) || Boolean.TYPE.equals(cls)) {
            obj = Boolean.valueOf(value);
        } else if (Number.class.isAssignableFrom(cls) || cls.isPrimitive()) {
            if (Integer.class.equals(cls) || Integer.TYPE.equals(cls)) {
                obj = Integer.valueOf(value);
            } else if (Long.class.equals(cls) || Long.TYPE.equals(cls)) {
                obj = Long.valueOf(value);
            } else if (Byte.class.equals(cls) || Byte.TYPE.equals(cls)) {
                obj = Byte.valueOf(value);
            } else if (Short.class.equals(cls) || Short.TYPE.equals(cls)) {
                obj = Short.valueOf(value);
            } else if (Float.class.equals(cls) || Float.TYPE.equals(cls)) {
                obj = Float.valueOf(value);
            } else if (Double.class.equals(cls) || Double.TYPE.equals(cls)) {
                obj = Double.valueOf(value);
            }
        } else if (cls.isEnum()) {
            obj = Enum.valueOf(cls.asSubclass(Enum.class), value);
        }

        return cls.cast(obj);
    }
}
