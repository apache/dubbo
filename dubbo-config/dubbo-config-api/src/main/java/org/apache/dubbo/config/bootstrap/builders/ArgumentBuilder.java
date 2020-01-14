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
package org.apache.dubbo.config.bootstrap.builders;

import org.apache.dubbo.config.ArgumentConfig;

/**
 * This is a builder for build {@link ArgumentConfig}.
 * @since 2.7
 */
public class ArgumentBuilder {
    /**
     * The argument index: index -1 represents not set
     */
    private Integer index = -1;

    /**
     * Argument type
     */
    private String type;

    /**
     * Whether the argument is the callback interface
     */
    private Boolean callback;

    public ArgumentBuilder index(Integer index) {
        this.index = index;
        return this;
    }

    public ArgumentBuilder type(String type) {
        this.type = type;
        return this;
    }

    public ArgumentBuilder callback(Boolean callback) {
        this.callback = callback;
        return this;
    }

    public ArgumentConfig build() {
        ArgumentConfig argumentConfig = new ArgumentConfig();
        argumentConfig.setIndex(index);
        argumentConfig.setType(type);
        argumentConfig.setCallback(callback);
        return argumentConfig;
    }
}
