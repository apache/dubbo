/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.dubbo.demo.consumer.comp;

import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.demo.ReactiveDemoService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Component("reactiveDemoServiceComponent")
public class ReactiveDemoServiceComponent implements ReactiveDemoService {
    @Reference
    private ReactiveDemoService reactiveDemoService;

    @Override
    public Mono<String> sayHello(String name) {
        return reactiveDemoService.sayHello(name);
    }

    @Override
    public Mono<List<String>> sayHellos(String name) {
        return reactiveDemoService.sayHellos(name);
    }

    @Override
    public Flux<String> namesList() {
        return reactiveDemoService.namesList();
    }

    @Override
    public Flux<List<String>> namesMatrix() {
        return reactiveDemoService.namesMatrix();
    }
}
