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
package org.apache.dubbo.common.utils.json;

public class TestObjectB {
    private Inner innerA;
    private Inner innerB;


    public Inner getInnerA() {
        return innerA;
    }

    public void setInnerA(Inner innerA) {
        this.innerA = innerA;
    }

    public Inner getInnerB() {
        return innerB;
    }

    public void setInnerB(Inner innerB) {
        this.innerB = innerB;
    }

    public static class Inner {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
