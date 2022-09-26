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

package org.apache.dubbo.errorcode.extractor;

import org.apache.dubbo.errorcode.model.LoggerMethodInvocation;
import org.apache.dubbo.errorcode.util.FileUtils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

/**
 * Unit tests of JdtBasedInvalidLoggerInvocationLocator.
 */
class JdtBasedInvalidLoggerInvocationLocatorTest {

    private static final JdtBasedInvalidLoggerInvocationLocator LOCATOR = new JdtBasedInvalidLoggerInvocationLocator();

    private static final String MOCK_SOURCE_NO_INVALID_INVOCATION = "mock-source/testing-mock-source/target/classes/org/apache/dubbo/errorcode/mock/NoInvalidInvocationSubClass.class";

    private static final String MOCK_SOURCE_HAS_INVALID_INVOCATION = "mock-source/testing-mock-source/target/classes/org/apache/dubbo/errorcode/mock/ParentClass.class";

    private static final String MOCK_SOURCE_LOGGING_FIELD_IN_SUPER_CLASS = "mock-source/testing-mock-source/target/classes/org/apache/dubbo/errorcode/mock/SubClass.class";

    private static final String MOCK_SOURCE_LOGGING_FIELD_IN_GRAND_SUPER_CLASS = "mock-source/testing-mock-source/target/classes/org/apache/dubbo/errorcode/mock/GrandChildClass.class";

    private static final String MOCK_SOURCE_LOGGING_FIELD_IN_SUPER_CLASS_IN_DIFFERENT_PACKAGE = "mock-source/testing-mock-source/target/classes/org/apache/dubbo/errorcode/mock/diffpack/DifferentPackageSubClass.class";

    @Test
    void testSourceHasInvalidInvocation() {

        String mockSourceAbsolutePath = FileUtils.getResourceFilePath(MOCK_SOURCE_HAS_INVALID_INVOCATION)
            .replace("/", File.separator);

        List<LoggerMethodInvocation> methodInvocationList = LOCATOR.locateInvalidLoggerInvocation(mockSourceAbsolutePath);

        // When copying a code snippet to the testing code,
        // Remove the ';' (semicolon) symbol, and any space around comma (',').
        Assertions.assertTrue(
            methodInvocationList
                .stream()
                .anyMatch(x -> x.getLoggerMethodInvocationCode().equals("logger.warn(\"Testing in ParentClass! \")"))
        );
    }

    @Test
    void testSourceHasNoInvalidInvocation() {

        String mockSourceAbsolutePath = FileUtils.getResourceFilePath(MOCK_SOURCE_NO_INVALID_INVOCATION)
            .replace("/", File.separator);

        List<LoggerMethodInvocation> methodInvocationList = LOCATOR.locateInvalidLoggerInvocation(mockSourceAbsolutePath);

        Assertions.assertTrue(
            methodInvocationList.isEmpty()
        );
    }

    @Test
    void testLoggerFieldAppearsInSuperClass() {

        String mockSourceAbsolutePath = FileUtils.getResourceFilePath(MOCK_SOURCE_LOGGING_FIELD_IN_SUPER_CLASS)
            .replace("/", File.separator);

        List<LoggerMethodInvocation> methodInvocationList = LOCATOR.locateInvalidLoggerInvocation(mockSourceAbsolutePath);

        Assertions.assertTrue(
            methodInvocationList
                .stream()
                .anyMatch(x -> x.getLoggerMethodInvocationCode().equals("logger.warn(\"Testing in SubClass! \")"))
        );
    }

    @Test
    void testLoggerFieldAppearsInGrandSuperClass() {

        String mockSourceAbsolutePath = FileUtils.getResourceFilePath(MOCK_SOURCE_LOGGING_FIELD_IN_GRAND_SUPER_CLASS)
            .replace("/", File.separator);

        List<LoggerMethodInvocation> methodInvocationList = LOCATOR.locateInvalidLoggerInvocation(mockSourceAbsolutePath);

        Assertions.assertTrue(
            methodInvocationList
                .stream()
                .anyMatch(x -> x.getLoggerMethodInvocationCode().equals("logger.warn(msg)"))
        );
    }

    @Test
    void testLoggerFieldAppearsInDifferentPackage() {
        String mockSourceAbsolutePath = FileUtils.getResourceFilePath(MOCK_SOURCE_LOGGING_FIELD_IN_SUPER_CLASS_IN_DIFFERENT_PACKAGE)
            .replace("/", File.separator);

        List<LoggerMethodInvocation> methodInvocationList = LOCATOR.locateInvalidLoggerInvocation(mockSourceAbsolutePath);

        Assertions.assertTrue(
            methodInvocationList
                .stream()
                .anyMatch(x -> x.getLoggerMethodInvocationCode().equals("logger.warn(\"Testing in DifferentPackageSubClass! \")"))
        );
    }
}
