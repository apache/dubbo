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
package org.apache.dubbo.rpc.support;

import org.apache.dubbo.rpc.CustomArgument;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface DemoService {
    void sayHello(String name);

    String echo(String text);

    long timestamp();

    String getThreadName();

    int getSize(String[] strs);

    int getSize(Object[] os);

    Object invoke(String service, String method) throws Exception;

    int stringLength(String str);

    Type enumlength(Type... types);

//	Type enumlength(Type type);

    String get(CustomArgument arg1);

    byte getbyte(byte arg);

    Person getPerson(Person person);

    String testReturnType(String str);

    List<String> testReturnType1(String str);

    CompletableFuture<String> testReturnType2(String str);

    CompletableFuture<List<String>> testReturnType3(String str);

    CompletableFuture testReturnType4(String str);

    CompletableFuture<Map<String, String>> testReturnType5(String str);

    void $invoke(String s1, String s2);

}