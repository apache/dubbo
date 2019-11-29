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
package org.apache.dubbo.samples.microservices.sc.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RefreshScope
@RestController
@RequestMapping("/property")
public class PropertyController {

    @Value("${applicationProp:default value}")
    private String applicationProperty;

    @Value("${property:default value}")
    private String property;

    @Value("${property2:default value}")
    private String property2;

    @Value("${infrastructureDesc:default value}")
    private String infrastructureDesc;

    @Value("${infrastructureUserName:default value}")
    private String infrastructureUserName;

    @Value("${infrastructurePassword:default value}")
    private String infrastructurePassword;

    @Value("${password:mypassword}")
    private String secret;

    @RequestMapping
    public String getProperty() {
        return "<h3>applicationProperty:</h3> " + applicationProperty + "</br>"
                + "<h3>property:</h3> " + property + "</br>"
                + "<h3>property2:</h3> " + property2 + "</br>"
                + "<h3>infrastructureDesc:</h3> " + infrastructureDesc + "</br>"
                + "<h3>infrastructureUserName:</h3> " + infrastructureUserName + "</br>"
                + "<h3>infrastructurePassword:</h3> " + infrastructurePassword + "</br>"
                + "<h3>vault password:</h3>" + secret + "</br>";
    }

}
