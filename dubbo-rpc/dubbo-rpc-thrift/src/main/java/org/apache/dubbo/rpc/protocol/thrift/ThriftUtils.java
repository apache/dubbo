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
package org.apache.dubbo.rpc.protocol.thrift;
/**
 * @since 2.7.0, use https://github.com/dubbo/dubbo-rpc-native-thrift instead
 */
@Deprecated
public class ThriftUtils {

    /**
     * Generate class name which represents service arguments.
     *
     * @param serviceName service name
     * @param methodName method name
     * @return method args class name or null
     */
    public static String generateMethodArgsClassName(String serviceName, String methodName) {

        int index = serviceName.lastIndexOf(".");

        if (index > 0) {

            return serviceName.substring(0, index + 1) +
                    "$__" +
                    serviceName.substring(index + 1) +
                    "Stub$" +
                    methodName +
                    "_args";

        } else {
            return "$__" +
                    serviceName +
                    "Stub$" +
                    methodName +
                    "_args";
        }

    }

    public static String generateMethodResultClassName(String serviceName, String method) {

        int index = serviceName.lastIndexOf(".");

        if (index > 0) {

            return serviceName.substring(0, index + 1) +
                    "$__" +
                    serviceName.substring(index + 1) +
                    "Stub$" +
                    method +
                    "_result";

        } else {
            return "$__" +
                    serviceName +
                    "Stub$" +
                    method +
                    "_result";
        }

    }

    public static String generateSetMethodName(String fieldName) {

        return "set" +
                Character.toUpperCase(fieldName.charAt(0)) +
                fieldName.substring(1);

    }

    public static String generateGetMethodName(String fieldName) {
        return "get" +
                Character.toUpperCase(fieldName.charAt(0)) +
                fieldName.substring(1);
    }

    public static String generateMethodArgsClassNameThrift(String serviceName, String methodName) {

        int index = serviceName.indexOf("$");

        if (index > 0) {
            return serviceName.substring(0, index + 1) +
                    methodName +
                    "_args";
        }

        return null;

    }

    public static String generateMethodResultClassNameThrift(String serviceName, String methodName) {

        int index = serviceName.indexOf("$");

        if (index > 0) {
            return serviceName.substring(0, index + 1) +
                    methodName +
                    "_result";
        }

        return null;

    }

}
