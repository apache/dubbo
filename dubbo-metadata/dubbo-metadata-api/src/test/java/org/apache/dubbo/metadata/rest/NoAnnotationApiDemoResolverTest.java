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
package org.apache.dubbo.metadata.rest;

import org.apache.dubbo.metadata.rest.jaxrs.JAXRSServiceRestMetadataResolver;
import org.apache.dubbo.metadata.rest.springmvc.SpringMvcServiceRestMetadataResolver;
import org.apache.dubbo.rpc.model.ApplicationModel;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

public class NoAnnotationApiDemoResolverTest {
    private JAXRSServiceRestMetadataResolver jaxrsServiceRestMetadataResolver =
            new JAXRSServiceRestMetadataResolver(ApplicationModel.defaultModel());
    private SpringMvcServiceRestMetadataResolver springMvcServiceRestMetadataResolver =
            new SpringMvcServiceRestMetadataResolver(ApplicationModel.defaultModel());

    @Test
    void testNoAnnotationApiResolver() {
        Assertions.assertTrue(jaxrsServiceRestMetadataResolver.supports(JaxrsNoAnnotationApiDemoImpl.class));
        Assertions.assertTrue(springMvcServiceRestMetadataResolver.supports(SpringMvcNoAnnotationApiDemoImpl.class));
    }
}

class JaxrsNoAnnotationApiDemoImpl implements NoAnnotationApiDemo {
    @Override
    @Path("/test")
    @GET
    public String test(@QueryParam("test") String test) {
        return "success" + test;
    }
}

class SpringMvcNoAnnotationApiDemoImpl implements NoAnnotationApiDemo {
    @Override
    @RequestMapping("/test")
    public String test(@RequestBody() String test) {
        return "success" + test;
    }
}

interface NoAnnotationApiDemo {

    String test(String test);
}
