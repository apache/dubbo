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
package org.apache.dubbo.spring.boot.observability.autoconfigure;

import static org.apache.dubbo.spring.boot.util.DubboUtils.DUBBO_PREFIX;
import static org.apache.dubbo.spring.boot.util.DubboUtils.PROPERTY_NAME_SEPARATOR;

/**
 * The utilities class for Dubbo Observability
 *
 * @since 3.2.0
 */
public class ObservabilityUtils {

    public static final String DUBBO_TRACING_PREFIX = DUBBO_PREFIX + PROPERTY_NAME_SEPARATOR + "tracing" + PROPERTY_NAME_SEPARATOR;

    public static final String DUBBO_TRACING_PROPAGATION = DUBBO_TRACING_PREFIX + "propagation";

    public static final String DUBBO_TRACING_BAGGAGE = DUBBO_TRACING_PREFIX + "baggage";

    public static final String DUBBO_TRACING_BAGGAGE_CORRELATION = DUBBO_TRACING_BAGGAGE + PROPERTY_NAME_SEPARATOR + "correlation";

    public static final String DUBBO_TRACING_BAGGAGE_ENABLED = DUBBO_TRACING_BAGGAGE + PROPERTY_NAME_SEPARATOR + "enabled";

}
