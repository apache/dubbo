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
package org.apache.dubbo.remoting.transport.netty4;

import java.util.LinkedHashMap;
import java.util.Map;

public class TransporterConfig {

    private final Map<ConfigOption<?>, Object> options = new LinkedHashMap<ConfigOption<?>, Object>();

    public <T> TransporterConfig option(ConfigOption<T> option, T value) {
        if (option == null) {
            throw new NullPointerException("option");
        }
        if (value == null) {
            synchronized (options) {
                options.remove(option);
            }
        } else {
            synchronized (options) {
                options.put(option, value);
            }
        }
        return this;
    }

    public <T> T option(ConfigOption<T> option) {
        if (option == null) {
            throw new NullPointerException("option");
        }
        return (T) options.get(option);
    }

    public Map<ConfigOption<?>, Object> options() {
        return options;
    }
}
