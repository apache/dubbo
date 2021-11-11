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

import org.apache.log4j.Level;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class LogTest {
    @Test
    public void testLogName() throws Exception {
        Log log1 = new Log();
        Log log2 = new Log();
        Log log3 = new Log();
        log1.setLogName("log-name");
        log2.setLogName("log-name");
        log3.setLogName("log-name-other");
        assertThat(log1.getLogName(), equalTo("log-name"));
        Assertions.assertEquals(log1, log2);
        Assertions.assertEquals(log1.hashCode(), log2.hashCode());
        Assertions.assertNotEquals(log1, log3);
    }

    @Test
    public void testLogLevel() throws Exception {
        Log log1 = new Log();
        Log log2 = new Log();
        Log log3 = new Log();
        log1.setLogLevel(Level.ALL);
        log2.setLogLevel(Level.ALL);
        log3.setLogLevel(Level.DEBUG);
        assertThat(log1.getLogLevel(), is(Level.ALL));
        Assertions.assertEquals(log1, log2);
        Assertions.assertEquals(log1.hashCode(), log2.hashCode());
        Assertions.assertNotEquals(log1, log3);
    }

    @Test
    public void testLogMessage() throws Exception {
        Log log1 = new Log();
        Log log2 = new Log();
        Log log3 = new Log();
        log1.setLogMessage("log-message");
        log2.setLogMessage("log-message");
        log3.setLogMessage("log-message-other");
        assertThat(log1.getLogMessage(), equalTo("log-message"));
        Assertions.assertEquals(log1, log2);
        Assertions.assertEquals(log1.hashCode(), log2.hashCode());
        Assertions.assertNotEquals(log1, log3);
    }

    @Test
    public void testLogThread() throws Exception {
        Log log1 = new Log();
        Log log2 = new Log();
        Log log3 = new Log();
        log1.setLogThread("log-thread");
        log2.setLogThread("log-thread");
        log3.setLogThread("log-thread-other");
        assertThat(log1.getLogThread(), equalTo("log-thread"));
        Assertions.assertEquals(log1, log2);
        Assertions.assertEquals(log1.hashCode(), log2.hashCode());
        Assertions.assertNotEquals(log1, log3);
    }

}
