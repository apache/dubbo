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
package org.apache.dubbo.common.json.impl;

import org.apache.dubbo.common.extension.DisableInject;
import org.apache.dubbo.common.json.JsonUtilCustomizer;

public abstract class CustomizableJsonUtil<FIRST, SECOND> extends AbstractJsonUtilImpl {

    private JsonUtilCustomizer<Object> customizer;

    private volatile FIRST first;
    private volatile SECOND second;

    @DisableInject
    public final void setCustomizer(JsonUtilCustomizer<Object> customizer) {
        this.customizer = customizer;
    }

    public final boolean hasCustomizer() {
        return customizer != null;
    }

    public final FIRST getFirst() {
        FIRST first = this.first;
        if (first == null) {
            synchronized (this) {
                first = this.first;
                if (first == null) {
                    this.first = first = createFirst();
                }
            }
        }
        return first;
    }

    public final SECOND getSecond() {
        SECOND second = this.second;
        if (second == null) {
            synchronized (this) {
                second = this.second;
                if (second == null) {
                    this.second = second = createSecond();
                }
            }
        }
        return second;
    }

    protected FIRST createFirst() {
        FIRST first = newFirst();
        if (customizer != null) {
            customizer.customize(first);
        }
        return first;
    }

    protected SECOND createSecond() {
        SECOND second = newSecond();
        if (customizer != null) {
            customizer.customize(second);
        }
        return second;
    }

    protected FIRST newFirst() {
        throw new UnsupportedOperationException();
    }

    protected SECOND newSecond() {
        throw new UnsupportedOperationException();
    }
}
