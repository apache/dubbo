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
package com.service;

import com.pojo.Demo1;
import com.pojo.Demo2;
import com.pojo.Demo4;
import com.pojo.Demo5;
import com.pojo.Demo6;
import com.pojo.Demo7;
import com.pojo.Demo8;
import com.pojo.DemoException1;
import com.pojo.DemoException3;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public interface DemoService1<T extends Demo8> {
    Demo1 getDemo1();

    void setDemo2(Demo2 demo2);

    List<Demo4> getDemo4s();

    List<HashSet<LinkedList<Set<Vector<Map<? extends Demo5, ? super Demo6>>>>>> getDemo5s();

    List<Demo7>[] getDemo7s();

    List<T> getTs();

    void echo1() throws DemoException1;

    void echo2() throws DemoException3;
}
