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
package org.apache.dubbo.rpc.protocol.rest.mvc;


import org.apache.dubbo.rpc.protocol.rest.User;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RequestMapping("/demoService")
public interface SpringRestDemoService {
    @RequestMapping(value = "/hello", method = RequestMethod.GET)
    Integer hello(@RequestParam Integer a, @RequestParam Integer b);

    @RequestMapping(value = "/error", method = RequestMethod.GET)
    String error();

    @RequestMapping(value = "/sayHello", method = RequestMethod.POST, consumes = MediaType.TEXT_PLAIN_VALUE)
    String sayHello(String name);

    boolean isCalled();

    @RequestMapping(value = "/testFormBody", method = RequestMethod.POST, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String testFormBody(@RequestBody User user);

    @RequestMapping(value = "/testFormMapBody", method = RequestMethod.POST, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    List<String> testFormMapBody(@RequestBody LinkedMultiValueMap map);

    @RequestMapping(value = "/testHeader", method = RequestMethod.POST, consumes = MediaType.TEXT_PLAIN_VALUE)
    String testHeader(@RequestHeader String header);

    @RequestMapping(value = "/testHeaderInt", method = RequestMethod.GET, consumes = MediaType.TEXT_PLAIN_VALUE)
    String testHeaderInt(@RequestHeader int header);

    @RequestMapping(method = RequestMethod.GET, value = "/primitive")
    int primitiveInt(@RequestParam("a") int a, @RequestParam("b") int b);

    @RequestMapping(method = RequestMethod.GET, value = "/primitiveLong")
    long primitiveLong(@RequestParam("a") long a, @RequestParam("b") Long b);

    @RequestMapping(method = RequestMethod.GET, value = "/primitiveByte")
    long primitiveByte(@RequestParam("a") byte a, @RequestParam("b") Long b);


    @RequestMapping(method = RequestMethod.POST, value = "/primitiveShort")
    long primitiveShort(@RequestParam("a") short a, @RequestParam("b") Long b, @RequestBody int c);
}
