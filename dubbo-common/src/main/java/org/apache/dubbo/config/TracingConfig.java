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
package org.apache.dubbo.config;

import org.apache.dubbo.config.nested.BaggageConfig;
import org.apache.dubbo.config.nested.PropagationConfig;
import org.apache.dubbo.config.nested.SamplingConfig;
import org.apache.dubbo.config.support.Nested;
import org.apache.dubbo.rpc.model.ApplicationModel;

/**
 * TracingConfig
 */
public class TracingConfig extends AbstractConfig {

    private static final long serialVersionUID = -9089919311611546383L;

    private Boolean enabled = false;

    /**
     * Sampling configuration.
     */
    @Nested
    private SamplingConfig sampling = new SamplingConfig();

    /**
     * Baggage configuration.
     */
    @Nested
    private BaggageConfig baggage = new BaggageConfig();

    /**
     * Propagation configuration.
     */
    @Nested
    private PropagationConfig propagation = new PropagationConfig();

    public TracingConfig() {
    }

    public TracingConfig(ApplicationModel applicationModel) {
        super(applicationModel);
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public SamplingConfig getSampling() {
        return sampling;
    }

    public void setSampling(SamplingConfig sampling) {
        this.sampling = sampling;
    }

    public BaggageConfig getBaggage() {
        return baggage;
    }

    public void setBaggage(BaggageConfig baggage) {
        this.baggage = baggage;
    }

    public PropagationConfig getPropagation() {
        return propagation;
    }

    public void setPropagation(PropagationConfig propagation) {
        this.propagation = propagation;
    }
}
