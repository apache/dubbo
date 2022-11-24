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
package org.apache.dubbo.metadata.tools;

import org.apache.dubbo.metadata.rest.RestService;
import org.apache.dubbo.metadata.rest.SpringRestService;
import org.apache.dubbo.metadata.rest.User;

import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * {@link SpringRestService} Test
 *
 * @since 2.7.6
 */
class SpringRestServiceTest {

    @Test
    void test() throws IOException {
        Compiler compiler = new Compiler();
        compiler.compile(User.class, RestService.class, SpringRestService.class);
    }
}
