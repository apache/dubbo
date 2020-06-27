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
package org.apache.dubbo.monitor.dubbo;

import org.apache.dubbo.rpc.AppResponse;

import java.util.Map;

public class AppResponseBuilder {
    private Object result;
    private Throwable exception;
    private Map<String, String> attachments;
    private AppResponse appResponse;

    private AppResponseBuilder() {
        this.appResponse = new AppResponse();
    }

    public static AppResponseBuilder create() {
        return new AppResponseBuilder();
    }

    public AppResponse build() {
        return new AppResponse(this);
    }

    public AppResponseBuilder withResult(Object result) {
        this.result = result;
        return this;
    }

    public AppResponseBuilder withException(Throwable exception) {
        this.exception = exception;
        return this;
    }

    public AppResponseBuilder withAttachments(Map<String, String> attachments) {
        this.attachments = attachments;
        return this;
    }
}
