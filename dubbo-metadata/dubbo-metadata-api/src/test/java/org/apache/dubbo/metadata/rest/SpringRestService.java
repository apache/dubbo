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

import org.apache.dubbo.config.annotation.DubboService;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring MVC {@link RestService}
 *
 * @since 2.7.6
 */
@DubboService(version = "2.0.0", group = "spring")
@RestController
public class SpringRestService implements RestService {

    @Override
    @GetMapping(value = "/param")
    public String param(@RequestParam(defaultValue = "value-param") String param) {
        return param;
    }

    @Override
    @PostMapping("/params")
    public String params(@RequestParam(defaultValue = "value-a") int a, @RequestParam(defaultValue = "value-b") String b) {
        return a + b;
    }

    @Override
    @GetMapping("/headers")
    public String headers(@RequestHeader(name = "h", defaultValue = "value-h") String header,
                          @RequestHeader(name = "h2", defaultValue = "value-h2") String header2,
                          @RequestParam(value = "v", defaultValue = "1") Integer param) {
        String result = header + " , " + header2 + " , " + param;
        return result;
    }

    @Override
    @GetMapping("/path-variables/{p1}/{p2}")
    public String pathVariables(@PathVariable("p1") String path1,
                                @PathVariable("p2") String path2, @RequestParam("v") String param) {
        String result = path1 + " , " + path2 + " , " + param;
        return result;
    }

    @Override
    @PostMapping("/form")
    public String form(@RequestParam("f") String form) {
        return String.valueOf(form);
    }

    @Override
    @PostMapping(value = "/request/body/map", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public User requestBodyMap(@RequestBody Map<String, Object> data,
                               @RequestParam("param") String param) {
        User user = new User();
        user.setId(((Integer) data.get("id")).longValue());
        user.setName((String) data.get("name"));
        user.setAge((Integer) data.get("age"));
        return user;
    }

    @PostMapping(value = "/request/body/user", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @Override
    public Map<String, Object> requestBodyUser(@RequestBody User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("name", user.getName());
        map.put("age", user.getAge());
        return map;
    }
}
