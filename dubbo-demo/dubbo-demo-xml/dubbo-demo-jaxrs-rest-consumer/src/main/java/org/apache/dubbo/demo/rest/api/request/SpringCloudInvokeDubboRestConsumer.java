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
package org.apache.dubbo.demo.rest.api.request;

import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.demo.rest.api.HttpService;
import org.springframework.stereotype.Component;


/**
 *  if your service is spring cloud ,you want to request dubbo rest service
 *
 *  only if add the dependency of
 *
 *          <dependency>
 *             <groupId>org.apache.dubbo</groupId>
 *             <artifactId>dubbo-rpc-rest</artifactId>
 *         </dependency>
 *
 *    and set the dubbo invoke url
 */
@Component
public class SpringCloudInvokeDubboRestConsumer {

    /**
     *  URL rest://localhost:8888/services
     *   rest protocol
     *   localhost:8888 server address
     *   services context path
     */
    @DubboReference(interfaceClass = HttpService.class ,url = "rest://localhost:8888/services",version = "1.0.0",group = "test")
    HttpService httpService;

    public void invokeHttpService() {
        String http = httpService.http("Spring Cloud Invoke Dubbo Rest");
        System.out.println(http);
    }


}
