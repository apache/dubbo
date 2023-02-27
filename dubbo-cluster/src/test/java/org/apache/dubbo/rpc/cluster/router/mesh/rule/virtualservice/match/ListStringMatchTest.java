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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListStringMatchTest {

    @Test
    void isMatch() {
        ListStringMatch listStringMatch = new ListStringMatch();

        List<StringMatch> oneof = new ArrayList<>();

        StringMatch stringMatch1 = new StringMatch();
        stringMatch1.setExact("1");

        StringMatch stringMatch2 = new StringMatch();
        stringMatch2.setExact("2");

        oneof.add(stringMatch1);
        oneof.add(stringMatch2);


        listStringMatch.setOneof(oneof);

        assertTrue(listStringMatch.isMatch("1"));
        assertTrue(listStringMatch.isMatch("2"));
        assertFalse(listStringMatch.isMatch("3"));

    }
}
