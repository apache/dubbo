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

            return new StringBuilder(32)
                    .append(serviceName, 0, index + 1)
                    .append("$__")
                    .append(serviceName.substring(index + 1))
                    .append("Stub$")
                    .append(methodName)
                    .append("_args")
                    .toString();

        } else {
            return new StringBuffer(32)
                    .append("$__")
                    .append(serviceName)
                    .append("Stub$")
                    .append(methodName)
                    .append("_args")
                    .toString();
        }

    }

    public static String generateMethodResultClassName(String serviceName, String method) {

        int index = serviceName.lastIndexOf(".");

        if (index > 0) {

            return new StringBuilder(32)
                    .append(serviceName, 0, index + 1)
                    .append("$__")
                    .append(serviceName.substring(index + 1))
                    .append("Stub$")
                    .append(method)
                    .append("_result")
                    .toString();

        } else {
            return new StringBuilder(32)
                    .append("$__")
                    .append(serviceName)
                    .append("Stub$")
                    .append(method)
                    .append("_result")
                    .toString();
        }

    }

    public static String generateSetMethodName(String fieldName) {

        return new StringBuilder(16)
                .append("set")
                .append(Character.toUpperCase(fieldName.charAt(0)))
                .append(fieldName.substring(1))
                .toString();

    }

    public static String generateGetMethodName(String fieldName) {
        return new StringBuffer(16)
                .append("get")
                .append(Character.toUpperCase(fieldName.charAt(0)))
                .append(fieldName.substring(1))
                .toString();
    }

    public static String generateMethodArgsClassNameThrift(String serviceName, String methodName) {

        int index = serviceName.indexOf("$");

        if (index > 0) {
            return new StringBuilder(32)
                    .append(serviceName, 0, index + 1)
                    .append(methodName)
                    .append("_args")
                    .toString();
        }

        return null;

    }

    public static String generateMethodResultClassNameThrift(String serviceName, String methodName) {

        int index = serviceName.indexOf("$");

        if (index > 0) {
            return new StringBuilder(32)
                    .append(serviceName, 0, index + 1)
                    .append(methodName)
                    .append("_result")
                    .toString();
        }

        return null;

    }

}
