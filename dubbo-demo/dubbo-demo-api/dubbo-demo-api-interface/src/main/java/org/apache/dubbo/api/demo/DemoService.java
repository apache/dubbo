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
package org.apache.dubbo.api.demo;

import java.util.concurrent.CompletableFuture;

public interface DemoService {

    String sayHello(String name);

    /**
     * Asynchronous method example.
     * <p>
     * This method returns a {@link CompletableFuture<String>} to demonstrate Dubbo's asynchronous invocation capability.
     * Developers are recommended to refer to the official sample project for complete usage:
     * <a href="https://github.com/lqscript/dubbo-samples/tree/master/2-advanced/dubbo-samples-async/dubbo-samples-async-original-future">
     *     Dubbo Async Invocation Sample</a>
     * </p>
     *
     * @param name Input name parameter
     * @return Asynchronous result wrapped in CompletableFuture
     */
    default CompletableFuture<String> sayHelloAsync(String name) {
        return CompletableFuture.completedFuture(sayHello(name));
    }
}
