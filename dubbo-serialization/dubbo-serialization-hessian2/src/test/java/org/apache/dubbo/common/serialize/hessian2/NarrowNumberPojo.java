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
package org.apache.dubbo.common.serialize.hessian2;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class NarrowNumberPojo implements Serializable {

    private String name;
    private byte age;
    private short height;
    private float salary;
    private List<Byte> scores;
    private Map<String, Byte> attributes;

    public NarrowNumberPojo() {}

    public NarrowNumberPojo(
            String name, byte age, short height, float salary, List<Byte> scores, Map<String, Byte> attributes) {
        this.name = name;
        this.age = age;
        this.height = height;
        this.salary = salary;
        this.scores = scores;
        this.attributes = attributes;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte getAge() {
        return age;
    }

    public void setAge(byte age) {
        this.age = age;
    }

    public short getHeight() {
        return height;
    }

    public void setHeight(short height) {
        this.height = height;
    }

    public float getSalary() {
        return salary;
    }

    public void setSalary(float salary) {
        this.salary = salary;
    }

    public List<Byte> getScores() {
        return scores;
    }

    public void setScores(List<Byte> scores) {
        this.scores = scores;
    }

    public Map<String, Byte> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Byte> attributes) {
        this.attributes = attributes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NarrowNumberPojo that = (NarrowNumberPojo) o;
        return age == that.age
                && height == that.height
                && Float.compare(that.salary, salary) == 0
                && Objects.equals(name, that.name)
                && Objects.equals(scores, that.scores)
                && Objects.equals(attributes, that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, age, height, salary, scores, attributes);
    }
}
