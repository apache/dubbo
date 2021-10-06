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

package org.apache.dubbo.metrics.prometheus;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.common.metrics.model.sample.MetricSample;
import org.apache.dubbo.metrics.AbstractMetricsReporter;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Tag;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Metrics reporter for prometheus.
 */
public class PrometheusMetricsReporter extends AbstractMetricsReporter {

    public PrometheusMetricsReporter(URL url) {
        super(url);
    }

    @Override
    public void init() {
        PrometheusMeterRegistry meterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
            server.createContext("/metrics", httpExchange -> {
                String response = meterRegistry.scrape();
                httpExchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = httpExchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            });

            new Thread(server::start).start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // TODO 此处不需要executor，但是每次新增的metric需要写到meterRegistry才会生效
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(() -> {
            System.out.println("Start Collecting Metrics...");
            collectors.forEach(collector -> {
                List<MetricSample> samples = collector.collect();
                for (MetricSample sample : samples) {
                    if (sample.getType() == MetricSample.Type.GAUGE) {
                        GaugeMetricSample gaugeSample = (GaugeMetricSample) sample;
                        List<Tag> tags = new ArrayList<>();
                        gaugeSample.getTags().forEach((k, v) -> tags.add(Tag.of(k, v)));

                        Gauge.builder(gaugeSample.getName(), gaugeSample.getSupplier())
                            .description(gaugeSample.getDescription()).tags(tags).register(meterRegistry);
                    } else {
                        // TODO
                    }
                }
            });
        }, 5000, 10000, TimeUnit.MILLISECONDS);

        // TODO
        System.out.println("initializing prometheus metrics reporter");
    }
}
