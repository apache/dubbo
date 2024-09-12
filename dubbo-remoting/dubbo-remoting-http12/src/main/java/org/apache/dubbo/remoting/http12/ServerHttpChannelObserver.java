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
package org.apache.dubbo.remoting.http12;

import org.apache.dubbo.common.stream.StreamObserver;

import java.util.function.BiConsumer;
import java.util.function.Function;

public interface ServerHttpChannelObserver<H extends HttpChannel> extends StreamObserver<Object>, AutoCloseable {

    H getHttpChannel();

    void addHeadersCustomizer(BiConsumer<HttpHeaders, Throwable> headersCustomizer);

    void addTrailersCustomizer(BiConsumer<HttpHeaders, Throwable> trailersCustomizer);

    void setExceptionCustomizer(Function<Throwable, ?> exceptionCustomizer);

    void close();
}
