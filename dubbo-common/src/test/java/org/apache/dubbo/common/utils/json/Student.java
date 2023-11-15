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
package org.apache.dubbo.common.utils.json;

import java.io.Serializable;
import java.util.List;

public class Student<W> implements Serializable {
    private Integer type;

    private List<String> names;

    private List<W> namesT;

    private W age;

    private String name;

    public Student(Integer type, String name) {
        this.type = type;
        this.name = name;
    }

    @Override
    public String toString() {
        return "Student{" + "type=" + type + ", name='" + name + '\'' + '}';
    }
}
