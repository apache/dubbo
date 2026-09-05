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
package org.apache.dubbo.config.nested;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TripleConfigTest {

    @Test
    void testMaxConnectionAgeDefaults() {
        TripleConfig config = new TripleConfig();
        Assertions.assertNull(config.getMaxConnectionAge());
        Assertions.assertEquals(-1L, config.getMaxConnectionAgeOrDefault());
        Assertions.assertNull(config.getMaxConnectionAgeGrace());
        Assertions.assertEquals(10_000L, config.getMaxConnectionAgeGraceOrDefault());
    }

    @Test
    void testMaxConnectionAgeSetAndGet() {
        TripleConfig config = new TripleConfig();
        config.setMaxConnectionAge(60_000L);
        config.setMaxConnectionAgeGrace(5_000L);
        Assertions.assertEquals(60_000L, config.getMaxConnectionAgeOrDefault());
        Assertions.assertEquals(5_000L, config.getMaxConnectionAgeGraceOrDefault());
    }

    @Test
    void testMaxConnectionAgeDisabledValue() {
        TripleConfig config = new TripleConfig();
        config.setMaxConnectionAge(-1L);
        Assertions.assertEquals(-1L, config.getMaxConnectionAgeOrDefault());
    }

    @Test
    void testMaxConnectionAgeValidation() {
        TripleConfig config = new TripleConfig();
        Assertions.assertThrows(IllegalArgumentException.class, () -> config.setMaxConnectionAge(0L));
        Assertions.assertThrows(IllegalArgumentException.class, () -> config.setMaxConnectionAge(-2L));
        Assertions.assertThrows(IllegalArgumentException.class, () -> config.setMaxConnectionAgeGrace(-1L));
    }
}
