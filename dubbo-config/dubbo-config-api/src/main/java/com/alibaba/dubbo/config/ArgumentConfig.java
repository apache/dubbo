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
package com.alibaba.dubbo.config;

import com.alibaba.dubbo.config.support.Parameter;

import java.io.Serializable;

/**
 * @export
 */
public class ArgumentConfig implements Serializable {

    private static final long serialVersionUID = -2165482463925213595L;

    //argument: index -1 represents not set
    private Integer index = -1;

    //argument type
    private String type;

    //callback interface
    private Boolean callback;

    @Parameter(excluded = true)
    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    @Parameter(excluded = true)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setCallback(Boolean callback) {
        this.callback = callback;
    }

    public Boolean isCallback() {
        return callback;
    }

}