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
public class DubboClassNameGenerator implements ClassNameGenerator {

    public static final String NAME = "dubbo";

    @Override
    public String generateArgsClassName(String serviceName, String methodName) {
        return ThriftUtils.generateMethodArgsClassName(serviceName, methodName);
    }

    @Override
    public String generateResultClassName(String serviceName, String methodName) {
        return ThriftUtils.generateMethodResultClassName(serviceName, methodName);
    }

}
