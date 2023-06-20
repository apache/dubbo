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
package org.apache.dubbo.config.nested;

import org.apache.dubbo.config.support.Nested;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BaggageConfig implements Serializable {

    private Boolean enabled = true;

    /**
     * Correlation configuration.
     */
    @Nested
    private Correlation correlation = new Correlation();

    /**
     * List of fields that are referenced the same in-process as it is on the wire.
     * For example, the field "x-vcap-request-id" would be set as-is including the
     * prefix.
     */
    private List<String> remoteFields = new ArrayList<>();

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Correlation getCorrelation() {
        return correlation;
    }

    public void setCorrelation(Correlation correlation) {
        this.correlation = correlation;
    }

    public List<String> getRemoteFields() {
        return remoteFields;
    }

    public void setRemoteFields(List<String> remoteFields) {
        this.remoteFields = remoteFields;
    }

    public static class Correlation implements Serializable {

        /**
         * Whether to enable correlation of the baggage context with logging contexts.
         */
        private boolean enabled = true;

        /**
         * List of fields that should be correlated with the logging context. That
         * means that these fields would end up as key-value pairs in e.g. MDC.
         */
        private List<String> fields = new ArrayList<>();

        public boolean isEnabled() {
            return this.enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getFields() {
            return this.fields;
        }

        public void setFields(List<String> fields) {
            this.fields = fields;
        }

    }
}
