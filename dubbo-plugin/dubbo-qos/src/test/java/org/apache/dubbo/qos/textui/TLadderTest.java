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
package org.apache.dubbo.qos.textui;

import org.junit.Test;


import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class TLadderTest {
    @Test
    public void testRendering() throws Exception {
        TLadder ladder = new TLadder();
        ladder.addItem("1");
        ladder.addItem("2");
        ladder.addItem("3");
        ladder.addItem("4");
        String result = ladder.rendering();
        String expected = "1\n" +
                "  `-2\n" +
                "    `-3\n" +
                "      `-4\n";
        assertThat(result, equalTo(expected));
        System.out.println(result);
    }
}
