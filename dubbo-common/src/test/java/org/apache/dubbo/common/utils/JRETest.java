/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.common.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.lang.model.SourceVersion;


class JRETest {

    @Test
    @Disabled
    void blankSystemVersion() {
        System.setProperty("java.version", "");
        JRE jre = JRE.currentVersion();
        Assertions.assertEquals(JRE.JAVA_8, jre);
    }

    @Test
    void testCurrentVersion() {
        // SourceVersion is an enum, which member name is RELEASE_XX.

        Assertions.assertEquals(SourceVersion.latest().name().split("_")[1],
            JRE.currentVersion().name().split("_")[1]);
    }
}
