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

package org.apache.dubbo.metrics.model.key;

import org.apache.dubbo.common.utils.Assert;

public class TypeWrapper {
    private final MetricsLevel level;
    private final Object postType;
    private final Object finishType;
    private final Object errorType;

    public TypeWrapper(MetricsLevel level, Object postType, Object finishType, Object errorType) {
        this.level = level;
        this.postType = postType;
        this.finishType = finishType;
        this.errorType = errorType;
    }

    public MetricsLevel getLevel() {
        return level;
    }

    public Object getErrorType() {
        return errorType;
    }

    public boolean isAssignableFrom(Object type) {
        Assert.notNull(type, "Type can not be null");
        return type.equals(postType) || type.equals(finishType) || type.equals(errorType);
    }
}
