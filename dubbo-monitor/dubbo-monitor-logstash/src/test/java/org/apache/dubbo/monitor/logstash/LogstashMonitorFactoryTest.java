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
package org.apache.dubbo.monitor.logstash;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.monitor.Monitor;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

public class LogstashMonitorFactoryTest {
    private LogstashMonitorFactory logstashMonitorFactory;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        this.logstashMonitorFactory = new LogstashMonitorFactory();
    }

    @Test
    public void testCreateMonitor() {
        URL urlWithoutPath = URL.valueOf("logstash://10.10.10.11:6666?format=json");
        Monitor monitor = logstashMonitorFactory.createMonitor(urlWithoutPath);
        assertThat(monitor, not(nullValue()));
        monitor.destroy();
    }

}
