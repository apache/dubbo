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
package org.apache.dubbo.common;

import org.apache.dubbo.common.utils.PojoUtils;

import org.junit.jupiter.api.Test;

public class PojoUtilsForNonPublicStaticTest {

    @Test
    public void testNonPublicStaticClass() {
        NonPublicStaticData nonPublicStaticData = new NonPublicStaticData("horizon");
        PojoUtils.generalize(nonPublicStaticData);
    }

    /**
     * the static class need is not same package with PojoUtils, so define it here.
     */
    static class NonPublicStaticData {

        private String name;

        public NonPublicStaticData(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
