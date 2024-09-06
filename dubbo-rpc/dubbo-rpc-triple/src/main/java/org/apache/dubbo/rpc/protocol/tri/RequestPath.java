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
package org.apache.dubbo.rpc.protocol.tri;

public final class RequestPath {

    private final String path;
    private final String stubPath;
    private final String serviceInterface;
    private final String methodName;

    private RequestPath(String path, String stubPath, String serviceInterface, String methodName) {
        this.path = path;
        this.stubPath = stubPath;
        this.serviceInterface = serviceInterface;
        this.methodName = methodName;
    }

    // Request path patten:
    //     '{interfaceName}/{methodName}' or '{contextPath}/{interfaceName}/{methodName}'
    //      └─── path ────┘ └─ method ─┘      └────────── path ───────────┘ └─ method ─┘
    public static RequestPath parse(String fullPath) {
        int i = fullPath.lastIndexOf('/');
        if (i < 1) {
            return null;
        }

        String path = fullPath.substring(1, i);
        int j = path.lastIndexOf('/');
        if (j == -1) {
            return new RequestPath(path, fullPath, path, fullPath.substring(i + 1));
        } else {
            return new RequestPath(path, fullPath.substring(j), path.substring(j + 1), fullPath.substring(i + 1));
        }
    }

    public static String toFullPath(String path, String methodName) {
        return '/' + path + '/' + methodName;
    }

    public String getPath() {
        return path;
    }

    public String getStubPath() {
        return stubPath;
    }

    public String getServiceInterface() {
        return serviceInterface;
    }

    public String getMethodName() {
        return methodName;
    }

    @Override
    public String toString() {
        return path + '/' + methodName;
    }
}
