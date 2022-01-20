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

public class DoubleMatchTest {

    @Test
    public void exactMatch() {
        DoubleMatch doubleMatch = new DoubleMatch();
        doubleMatch.setExact(10.0);

        assertTrue(doubleMatch.isMatch(10.0));
        assertFalse(doubleMatch.isMatch(9.0));
    }

    @Test
    public void rangeStartMatch() {
        DoubleMatch doubleMatch = new DoubleMatch();

        DoubleRangeMatch doubleRangeMatch = new DoubleRangeMatch();
        doubleRangeMatch.setStart(10.0);

        doubleMatch.setRange(doubleRangeMatch);

        assertTrue(doubleMatch.isMatch(10.0));
        assertFalse(doubleMatch.isMatch(9.0));
    }


    @Test
    public void rangeEndMatch() {
        DoubleMatch doubleMatch = new DoubleMatch();

        DoubleRangeMatch doubleRangeMatch = new DoubleRangeMatch();
        doubleRangeMatch.setEnd(10.0);

        doubleMatch.setRange(doubleRangeMatch);

        assertFalse(doubleMatch.isMatch(10.0));
        assertTrue(doubleMatch.isMatch(9.0));
    }


    @Test
    public void rangeStartEndMatch() {
        DoubleMatch doubleMatch = new DoubleMatch();

        DoubleRangeMatch doubleRangeMatch = new DoubleRangeMatch();
        doubleRangeMatch.setStart(5.0);
        doubleRangeMatch.setEnd(10.0);

        doubleMatch.setRange(doubleRangeMatch);

        assertTrue(doubleMatch.isMatch(5.0));
        assertFalse(doubleMatch.isMatch(10.0));

        assertFalse(doubleMatch.isMatch(4.9));
        assertFalse(doubleMatch.isMatch(10.1));

        assertTrue(doubleMatch.isMatch(6.0));

    }

    @Test
    public void modMatch() {
        DoubleMatch doubleMatch = new DoubleMatch();

        doubleMatch.setMod(2.0);
        doubleMatch.setExact(3.0);

        assertFalse(doubleMatch.isMatch(3.0));

        doubleMatch.setExact(1.0);

        assertTrue(doubleMatch.isMatch(1.0));
        assertFalse(doubleMatch.isMatch(2.0));
        assertTrue(doubleMatch.isMatch(3.0));
    }

}
