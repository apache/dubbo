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
package org.apache.dubbo.maven.plugin.protoc.enums;

public enum DubboGenerateTypeEnum {
    Tri("tri", "org.apache.dubbo.gen.tri.Dubbo3TripleGenerator"),
    Tri_reactor("tri_reactor", "org.apache.dubbo.gen.tri.reactive.ReactorDubbo3TripleGenerator"),
    ;
    private String id;
    private String mainClass;

    DubboGenerateTypeEnum(String id, String mainClass) {
        this.id = id;
        this.mainClass = mainClass;
    }

    public static DubboGenerateTypeEnum getByType(String dubboGenerateType) {
        DubboGenerateTypeEnum[] values = DubboGenerateTypeEnum.values();
        for (DubboGenerateTypeEnum value : values) {
            if (value.getId().equals(dubboGenerateType)) {
                return value;
            }
        }
        return null;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }
}
