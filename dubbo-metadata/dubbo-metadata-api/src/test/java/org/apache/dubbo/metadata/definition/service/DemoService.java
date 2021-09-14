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
package org.apache.dubbo.metadata.definition.service;

import org.apache.dubbo.metadata.definition.service.annotation.MockMethodAnnotation;
import org.apache.dubbo.metadata.definition.service.annotation.MockMethodAnnotation2;
import org.apache.dubbo.metadata.definition.service.annotation.MockTypeAnnotation;

import java.util.List;

/**
 * for test
 */
@MockTypeAnnotation(666)
public interface DemoService {

    String complexCompute(String input, ComplexObject co);

    ComplexObject findComplexObject(String var1, int var2, long l, String[] var3, List<Integer> var4, ComplexObject.TestEnum testEnum);

    @MockMethodAnnotation(777)
    @MockMethodAnnotation2(888)
    void testAnnotation(boolean flag);
}
