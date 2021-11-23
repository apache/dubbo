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


public class ListDoubleMatchTest {

    @Test
    public void isMatch() {
        ListDoubleMatch listDoubleMatch = new ListDoubleMatch();
        List<DoubleMatch> oneof = new ArrayList<>();

        DoubleMatch doubleMatch1 = new DoubleMatch();
        doubleMatch1.setExact(10.0);

        DoubleMatch doubleMatch2 = new DoubleMatch();
        doubleMatch2.setExact(11.0);

        oneof.add(doubleMatch1);
        oneof.add(doubleMatch2);

        listDoubleMatch.setOneof(oneof);

        assertTrue(listDoubleMatch.isMatch(10.0));
        assertTrue(listDoubleMatch.isMatch(11.0));
        assertFalse(listDoubleMatch.isMatch(12.0));
    }
}
