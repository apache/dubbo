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
package org.apache.dubbo.xds.resource_new.update;

import org.apache.dubbo.common.utils.Assert;

import com.google.protobuf.Any;

public final class ParsedResource<T extends ResourceUpdate> {
    private final T resourceUpdate;
    private final Any rawResource;

    public ParsedResource(T resourceUpdate, Any rawResource) {
        Assert.notNull(resourceUpdate, "resourceUpdate must not be null");
        Assert.notNull(rawResource, "rawResource must not be null");
        this.resourceUpdate = resourceUpdate;
        this.rawResource = rawResource;
    }

    public T getResourceUpdate() {
        return resourceUpdate;
    }

    public Any getRawResource() {
        return rawResource;
    }
}
