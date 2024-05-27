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
package org.apache.dubbo.tracing.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PropagationTypeTest {

    @Test
    void forValue() {
        PropagationType propagationType1 = PropagationType.forValue("W3C");
        assertEquals(PropagationType.W3C, propagationType1);

        PropagationType propagationType2 = PropagationType.forValue("B3");
        assertEquals(PropagationType.B3, propagationType2);

        PropagationType propagationType3 = PropagationType.forValue("B33");
        assertNull(propagationType3);
    }
}
