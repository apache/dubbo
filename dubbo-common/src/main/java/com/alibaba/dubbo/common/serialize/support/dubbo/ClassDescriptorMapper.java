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
package com.alibaba.dubbo.common.serialize.support.dubbo;

/**
 * 类描述匹配器接口
 */
public interface ClassDescriptorMapper {

    /**
     * get Class-Descriptor by index.
     *
     * 根据类描述编号，获得类描述
     *
     * @param index index.
     * @return string.
     */
    String getDescriptor(int index);

    /**
     * get Class-Descriptor index
     *
     * 根据类描述，获得类描述编号
     *
     * @param desc Class-Descriptor
     * @return index.
     */
    int getDescriptorIndex(String desc);

}