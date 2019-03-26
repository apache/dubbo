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
package org.apache.dubbo.rpc.protocol.rest.integration.swagger;

import io.swagger.models.Swagger;
import org.jboss.resteasy.spi.ResteasyUriInfo;
import org.junit.Assert;
import org.junit.Test;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.*;

public class DubboSwaggerApiListingResourceTest {

    private Application app;
    private ServletConfig sc;

    @Test
    public void test() throws Exception {

        DubboSwaggerApiListingResource resource = new  DubboSwaggerApiListingResource();

        app = mock(Application.class);
        sc = mock(ServletConfig.class);
        Set<Class<?>> sets = new HashSet<Class<?>>();
        sets.add(SwaggerService.class);

        when(sc.getServletContext()).thenReturn(mock(ServletContext.class));
        when(app.getClasses()).thenReturn(sets);

        Response response = resource.getListingJson(app, sc,
                null, new ResteasyUriInfo(new URI("http://rest.test")));

        Assert.assertNotNull(response);
        Swagger swagger = (Swagger)response.getEntity();
        Assert.assertEquals("SwaggerService",swagger.getTags().get(0).getName());
        Assert.assertEquals("/demoService/hello",swagger.getPaths().keySet().toArray()[0].toString());
    }

}
