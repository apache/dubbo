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


public class ListBoolMatchTest {

    @Test
    public void isMatch() {
        ListBoolMatch listBoolMatch = new ListBoolMatch();
        List<BoolMatch> oneof = new ArrayList<>();

        BoolMatch boolMatch1 = new BoolMatch();
        boolMatch1.setExact(true);
        oneof.add(boolMatch1);
        listBoolMatch.setOneof(oneof);

        assertTrue(listBoolMatch.isMatch(true));
        assertFalse(listBoolMatch.isMatch(false));

        BoolMatch boolMatch2 = new BoolMatch();
        boolMatch2.setExact(false);
        oneof.add(boolMatch2);
        listBoolMatch.setOneof(oneof);

        assertTrue(listBoolMatch.isMatch(false));
    }
}
