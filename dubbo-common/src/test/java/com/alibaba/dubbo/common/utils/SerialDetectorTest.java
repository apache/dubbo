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
package com.alibaba.dubbo.common.utils;

import org.junit.Test;

import java.io.InvalidClassException;
import java.io.ObjectInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SerialDetectorTest {
    @Test
    public void testBlacklisted() throws Exception {
        try {
            ObjectInputStream stream = new SerialDetector(getClass().getResourceAsStream("/security/invalid_data.ser"));
            stream.readObject();
            fail();
        } catch (InvalidClassException expected) {
            assertTrue(expected.getMessage().contains("blocked"));
            assertTrue(expected.getMessage().contains("blacklist"));
            assertEquals(expected.classname, "org.hibernate.engine.spi.TypedValue");
        } catch (ClassNotFoundException e) {
            fail(e.getMessage());
        }
    }


}
