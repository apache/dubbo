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
package org.apache.dubbo.metadata.definition.common;

/**
 * 16/9/22.
 */
public interface TestService {
    /**
     * 入参为内部类
     *
     * @param innerClass
     * @return
     */
    void m1(OuterClass.InnerClass innerClass);

    /**
     * 入参为 int 数组
     *
     * @param a
     */
    void m2(int[] a);

    /**
     * 返回值自定义类型包含无泛型的 map
     *
     * @param s1
     * @return
     */
    ResultWithRawCollections m3(String s1);

    /**
     * 入参为枚举类型
     *
     * @param color
     */
    void m4(ColorEnum color);

    /**
     * 返回值自定义类型继承自Map
     *
     * @param s1
     * @return
     */
    ClassExtendsMap m5(String s1);
}
