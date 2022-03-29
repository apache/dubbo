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

package org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.match;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StringMatchTest {

    @Test
    public void exactMatch() {
        StringMatch stringMatch = new StringMatch();
        stringMatch.setExact("qinliujie");

        assertTrue(stringMatch.isMatch("qinliujie"));
        assertFalse(stringMatch.isMatch("other"));
        assertFalse(stringMatch.isMatch(null));
    }


    @Test
    public void prefixMatch() {
        StringMatch stringMatch = new StringMatch();
        stringMatch.setPrefix("org.apache.dubbo.rpc.cluster.router.mesh");

        assertTrue(stringMatch.isMatch("org.apache.dubbo.rpc.cluster.router.mesh.test"));
        assertFalse(stringMatch.isMatch("com.alibaba.hsf"));
        assertFalse(stringMatch.isMatch(null));
    }


    @Test
    public void regxMatch() {
        StringMatch stringMatch = new StringMatch();
        stringMatch.setRegex("org.apache.dubbo.rpc.cluster.router.mesh.*");

        assertTrue(stringMatch.isMatch("org.apache.dubbo.rpc.cluster.router.mesh"));
        assertTrue(stringMatch.isMatch("org.apache.dubbo.rpc.cluster.router.mesh.test"));
        assertFalse(stringMatch.isMatch("com.alibaba.hsf"));
        assertFalse(stringMatch.isMatch("com.taobao"));
    }


    @Test
    public void emptyMatch() {
        StringMatch stringMatch = new StringMatch();
        stringMatch.setEmpty("empty");

        assertFalse(stringMatch.isMatch("com.alibaba.hsf"));
        assertTrue(stringMatch.isMatch(""));
        assertTrue(stringMatch.isMatch(null));
    }

    @Test
    public void noEmptyMatch() {
        StringMatch stringMatch = new StringMatch();
        stringMatch.setNoempty("noempty");

        assertTrue(stringMatch.isMatch("com.alibaba.hsf"));
        assertFalse(stringMatch.isMatch(""));
        assertFalse(stringMatch.isMatch(null));
    }
}
