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
package org.apache.dubbo.tracing.tracer.brave;

import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.config.TracingConfig;
import org.apache.dubbo.config.nested.BaggageConfig;
import org.apache.dubbo.config.nested.ExporterConfig;
import org.apache.dubbo.config.nested.PropagationConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.tracing.utils.PropagationType;

import java.util.ArrayList;
import java.util.List;

import brave.propagation.Propagation;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BraveProviderTest {

    @Test
    void testGetTracer() {
        TracingConfig tracingConfig = new TracingConfig();
        tracingConfig.setEnabled(true);
        ExporterConfig exporterConfig = new ExporterConfig();
        exporterConfig.setZipkinConfig(new ExporterConfig.ZipkinConfig(""));
        tracingConfig.setTracingExporter(exporterConfig);

        BraveProvider braveProvider = new BraveProvider(ApplicationModel.defaultModel(), tracingConfig);
        Tracer tracer = braveProvider.getTracer();
        Assert.notNull(tracer, "Tracer should not be null.");
        assertEquals(io.micrometer.tracing.brave.bridge.BraveTracer.class, tracer.getClass());
    }

    @Test
    void testGetPropagator() {
        TracingConfig tracingConfig = mock(TracingConfig.class);
        PropagationConfig propagationConfig = mock(PropagationConfig.class);
        when(tracingConfig.getPropagation()).thenReturn(propagationConfig);
        BaggageConfig baggageConfig = mock(BaggageConfig.class);
        when(tracingConfig.getBaggage()).thenReturn(baggageConfig);

        // no baggage
        when(baggageConfig.getEnabled()).thenReturn(Boolean.FALSE);

        when(propagationConfig.getType()).thenReturn(PropagationType.W3C.getValue());
        Propagation.Factory w3cPropagationFactoryWithoutBaggage =
                BraveProvider.PropagatorFactory.getPropagationFactory(tracingConfig);
        assertEquals(
                io.micrometer.tracing.brave.bridge.W3CPropagation.class,
                w3cPropagationFactoryWithoutBaggage.getClass());

        when(propagationConfig.getType()).thenReturn(PropagationType.B3.getValue());
        Propagation.Factory b3PropagationFactoryWithoutBaggage =
                BraveProvider.PropagatorFactory.getPropagationFactory(tracingConfig);
        Assert.notNull(b3PropagationFactoryWithoutBaggage, "b3PropagationFactoryWithoutBaggage should not be null.");

        // with baggage
        when(baggageConfig.getEnabled()).thenReturn(Boolean.TRUE);
        BaggageConfig.Correlation correlation = mock(BaggageConfig.Correlation.class);
        when(correlation.isEnabled()).thenReturn(Boolean.TRUE);
        when(baggageConfig.getCorrelation()).thenReturn(correlation);
        List<String> remoteFields = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            remoteFields.add("test-hd-" + i);
        }
        when(baggageConfig.getRemoteFields()).thenReturn(remoteFields);
        Propagation.Factory propagationFactoryWithBaggage =
                BraveProvider.PropagatorFactory.getPropagationFactory(tracingConfig);
        Assert.notNull(propagationFactoryWithBaggage, "propagationFactoryWithBaggage should not be null.");
    }
}
