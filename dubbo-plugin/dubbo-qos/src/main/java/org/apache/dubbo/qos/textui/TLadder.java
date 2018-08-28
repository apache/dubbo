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
package org.apache.dubbo.qos.textui;

import java.util.ArrayList;
import java.util.List;

import static org.apache.dubbo.common.utils.StringUtils.repeat;

/**
 * Ladder
 */
public class TLadder implements TComponent {

    // separator
    private static final String LADDER_CHAR = "`-";

    // tab
    private static final String STEP_CHAR = " ";

    // indent length
    private static final int INDENT_STEP = 2;

    private final List<String> items = new ArrayList<String>();


    @Override
    public String rendering() {
        final StringBuilder ladderSB = new StringBuilder();
        int deep = 0;
        for (String item : items) {

            // no separator is required for the first item
            if (deep == 0) {
                ladderSB
                        .append(item)
                        .append("\n");
            }

            // need separator for others
            else {
                ladderSB
                        .append(repeat(STEP_CHAR, deep * INDENT_STEP))
                        .append(LADDER_CHAR)
                        .append(item)
                        .append("\n");
            }

            deep++;

        }
        return ladderSB.toString();
    }

    /**
     * add one item
     */
    public TLadder addItem(String item) {
        items.add(item);
        return this;
    }

}
