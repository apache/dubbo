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
package org.apache.dubbo.rpc.protocol.rest.annotation;


import org.apache.dubbo.metadata.rest.ArgInfo;

import java.util.List;

public class BaseParseContext {


    protected List<Object> args;

    protected List<ArgInfo> argInfos;


    public List<Object> getArgs() {
        return args;
    }

    public void setArgs(List<Object> args) {
        this.args = args;
    }

    public List<ArgInfo> getArgInfos() {
        return argInfos;
    }

    public void setArgInfos(List<ArgInfo> argInfos) {
        this.argInfos = argInfos;
    }

    public ArgInfo getArgInfoByIndex(int index) {
        return getArgInfos().get(index);
    }
}
