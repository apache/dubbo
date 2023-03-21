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

package org.apache.dubbo.annotation;

import org.apache.dubbo.annotation.util.FileUtils;
import org.apache.dubbo.eci.extractor.ErrorCodeExtractor;
import org.apache.dubbo.eci.extractor.JavassistConstantPoolErrorCodeExtractor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Real invocation test of DispatchingAnnotationProcessor (and DeprecatedHandler).
 */
class RealInvocationTest {

    private static final Map<String, Boolean> FILES = new HashMap<>(4, 1);

    static {
        FILES.put("TestConstructorMethod.java", true);
        FILES.put("TestDeprecatedMethod.java", true);
        FILES.put("TestInterfaceDeprecatedMethod.java", false);
        FILES.put("TestConstructorMethodParentClass.java", false);
        FILES.put("TestConstructorMethodSubClass.java", true);
    };

    @Test
    void test() {

        for (Map.Entry<String, Boolean> i : FILES.entrySet()) {
            String filePath = FileUtils.getResourceFilePath("org/testing/dm/" + i.getKey());

            Assertions.assertTrue(TestingCommons.compileTheSource(filePath), "Compile failed! ");

            ErrorCodeExtractor errorCodeExtractor = new JavassistConstantPoolErrorCodeExtractor();
            List<String> codes = errorCodeExtractor.getErrorCodes(filePath.replace(".java", ".class"));

            Assertions.assertEquals(i.getValue(), codes.contains("0-28"), i.getKey());
        }

    }
}
