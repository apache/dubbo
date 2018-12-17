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

import org.apache.dubbo.common.Constants;

import io.netty.util.AbstractConstant;
import io.netty.util.ConstantPool;

public class ConfigOption<T> extends AbstractConstant<ConfigOption<T>> {

    private static final ConstantPool<ConfigOption<Object>> pool = new ConstantPool<ConfigOption<Object>>() {
        @Override
        protected ConfigOption<Object> newConstant(int id, String name) {
            return new ConfigOption<Object>(id, name);
        }
    };

    public static final ConfigOption<Boolean> EPOLL = valueOf(Constants.EPOLL_ENABLE);

    public static final ConfigOption<Integer> IO_THREADS = valueOf(Constants.IO_THREADS_KEY);


    public static <T> ConfigOption<T> valueOf(String name) {
        return (ConfigOption<T>) pool.valueOf(name);
    }

    public static boolean exists(String name) {
        return pool.exists(name);
    }

    @SuppressWarnings("unchecked")
    public static <T> ConfigOption<T> newInstance(String name) {
        return (ConfigOption<T>) pool.newInstance(name);
    }

    private ConfigOption(int id, String name) {
        super(id, name);
    }
}
