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
package org.apache.dubbo.tracing.utils;

public enum PropagationType {
    W3C("W3C"),
    B3("B3");

    private final String value;

    PropagationType(String type) {
        this.value = type;
    }

    public String getValue() {
        return value;
    }

    public static PropagationType forValue(String value) {
        PropagationType[] values = values();
        for (PropagationType type : values) {
            if (type.getValue().equals(value)) {
                return type;
            }
        }
        return null;
    }
}
