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
package org.apache.dubbo.remoting.telnet;

import org.apache.dubbo.remoting.telnet.support.TelnetUtils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

class TelnetUtilsTest {

    /**
     * abc - abc - abc
     * 1   - 2   - 3
     * x   - y   - z
     */
    @Test
    void testToList() {
        List<List<String>> table = new LinkedList<>();
        table.add(Arrays.asList("abc","abc","abc"));
        table.add(Arrays.asList("1","2","3"));
        table.add(Arrays.asList("x","y","z"));

        String toList = TelnetUtils.toList(table);

        Assertions.assertTrue(toList.contains("abc - abc - abc"));
        Assertions.assertTrue(toList.contains("1   - 2   - 3"));
        Assertions.assertTrue(toList.contains("x   - y   - z"));
    }

    /**
     * +-----+-----+-----+
     * | A   | B   | C   |
     * +-----+-----+-----+
     * | abc | abc | abc |
     * | 1   | 2   | 3   |
     * | x   | y   | z   |
     * +-----+-----+-----+
     */
    @Test
    void testToTable() {
        List<List<String>> table = new LinkedList<>();
        table.add(Arrays.asList("abc","abc","abc"));
        table.add(Arrays.asList("1","2","3"));
        table.add(Arrays.asList("x","y","z"));

        String toTable = TelnetUtils.toTable(new String[]{"A","B","C"},table);

        Assertions.assertTrue(toTable.contains("| A   | B   | C   |"));
        Assertions.assertTrue(toTable.contains("| abc | abc | abc |"));
        Assertions.assertTrue(toTable.contains("| 1   | 2   | 3   |"));
        Assertions.assertTrue(toTable.contains("| x   | y   | z   |"));
    }
}
