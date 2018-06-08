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
package com.alibaba.dubbo.examples.rest;

import com.alibaba.dubbo.examples.rest.api.User;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class NonDubboRestConsumer {

    public static void main(String[] args) {
        String port = "8888";

        registerUser("http://localhost:" + port + "/services/users/register.json", MediaType.APPLICATION_JSON_TYPE);

        registerUser("http://localhost:" + port + "/services/users/register.xml", MediaType.TEXT_XML_TYPE);

        getUser("http://localhost:" + port + "/services/users/1.json");

        getUser("http://localhost:" + port + "/services/users/2.xml");

        registerUser("http://localhost:" + port + "/services/u/register.json", MediaType.APPLICATION_JSON_TYPE);

        registerUser("http://localhost:" + port + "/services/u/register.xml", MediaType.TEXT_XML_TYPE);

        getUser("http://localhost:" + port + "/services/u/1.json");

        getUser("http://localhost:" + port + "/services/u/2.xml");

        registerUser("http://localhost:" + port + "/services/customers/register.json", MediaType.APPLICATION_JSON_TYPE);

        registerUser("http://localhost:" + port + "/services/customers/register.xml", MediaType.TEXT_XML_TYPE);

        getUser("http://localhost:" + port + "/services/customers/1.json");

        getUser("http://localhost:" + port + "/services/customers/2.xml");
    }

    private static void registerUser(String url, MediaType mediaType) {
        System.out.println("Registering user via " + url);
        User user = new User(1L, "larrypage");
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(url);
        Response response = target.request().post(Entity.entity(user, mediaType));

        try {
            if (response.getStatus() != 200) {
                throw new RuntimeException("Failed with HTTP error code : " + response.getStatus());
            }
            System.out.println("Successfully got result: " + response.readEntity(String.class));
        } finally {
            response.close();
            client.close();
        }
    }

    private static void getUser(String url) {
        System.out.println("Getting user via " + url);
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(url);
        Response response = target.request().get();
        try {
            if (response.getStatus() != 200) {
                throw new RuntimeException("Failed with HTTP error code : " + response.getStatus());
            }
            System.out.println("Successfully got result: " + response.readEntity(String.class));
        } finally {
            response.close();
            client.close();
        }
    }
}
