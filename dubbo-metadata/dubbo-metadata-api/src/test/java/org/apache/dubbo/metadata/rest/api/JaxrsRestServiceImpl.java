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
package org.apache.dubbo.metadata.rest.api;

import org.apache.dubbo.metadata.rest.User;

import javax.ws.rs.core.MultivaluedMap;

public class JaxrsRestServiceImpl implements JaxrsRestService {


    @Override
    public String param(String param) {
        return param;
    }

    @Override
    public String header(String header) {
        return header;
    }

    @Override
    public User body(User user) {
        return user;
    }

    @Override
    public MultivaluedMap multiValue(MultivaluedMap map) {
        return map;
    }

    @Override
    public String pathVariable(String a) {
        return a;
    }

    @Override
    public String noAnno(String a) {
        return a;
    }
}
