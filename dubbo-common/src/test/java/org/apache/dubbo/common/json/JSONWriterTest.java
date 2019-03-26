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
package org.apache.dubbo.common.json;

import junit.framework.TestCase;

import java.io.StringWriter;

public class JSONWriterTest extends TestCase {
    public void testWriteJson() throws Exception {
        StringWriter w = new StringWriter();
        JSONWriter writer = new JSONWriter(w);

        writer.valueNull();
        assertEquals(w.getBuffer().toString(), "null");

        // write array.
        w.getBuffer().setLength(0);
        writer.arrayBegin().valueNull().valueBoolean(false).valueInt(16).arrayEnd();
        assertEquals(w.getBuffer().toString(), "[null,false,16]");

        // write object.
        w.getBuffer().setLength(0);
        writer.objectBegin().objectItem("type").valueString("org.apache.dubbo.TestService").objectItem("version").valueString("1.1.0").objectEnd();
        assertEquals(w.getBuffer().toString(), "{\"type\":\"org.apache.dubbo.TestService\",\"version\":\"1.1.0\"}");

        w.getBuffer().setLength(0);
        writer.objectBegin();
        writer.objectItem("name").objectItem("displayName");
        writer.objectItem("emptyList").arrayBegin().arrayEnd();
        writer.objectItem("list").arrayBegin().valueNull().valueBoolean(false).valueInt(16).valueString("stri'''ng").arrayEnd();
        writer.objectItem("service").objectBegin().objectItem("type").valueString("org.apache.dubbo.TestService").objectItem("version").valueString("1.1.0").objectEnd();
        writer.objectEnd();
    }
}