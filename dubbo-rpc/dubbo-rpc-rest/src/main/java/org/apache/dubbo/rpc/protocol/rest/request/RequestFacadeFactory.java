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
package org.apache.dubbo.rpc.protocol.rest.request;


public class RequestFacadeFactory {
    private final static String JakartaServlet = "jakarta.servlet.http.HttpServletRequest";
    private final static String JavaxServlet = "javax.servlet.http.HttpServletRequest";


    public static RequestFacade createRequestFacade(Object request) {

        if (tryLoad(JavaxServlet, request)) {

            return new JavaxServletRequestFacade(request);
        }

        if (tryLoad(JakartaServlet, request)) {
            return new JakartaServletRequestFacade(request);
        }


        throw new RuntimeException("no compatible  ServletRequestFacade and request type is " + request.getClass());

    }

    public static boolean tryLoad(String requestClassName, Object request) {

        ClassLoader classLoader = request.getClass().getClassLoader();

        try {
            Class<?> requestClass = classLoader.loadClass(requestClassName);

            return requestClass.isAssignableFrom(requestClass);

        } catch (ClassNotFoundException e) {
            return false;
        }

    }


}
