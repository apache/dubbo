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
package org.apache.dubbo.common.vo;

import java.util.Objects;

public class UserVo {
    private String name;
    private String addr;
    private int age;

    public UserVo(String name, String addr, int age) {
        this.name = name;
        this.addr = addr;
        this.age = age;
    }

    public UserVo() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public static UserVo getInstance() {
        return new UserVo("dubbo", "hangzhou", 10);
    }

    @Override
    public String toString() {
        return "UserVo{" +
            "name='" + name + '\'' +
            ", addr='" + addr + '\'' +
            ", age=" + age +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserVo userVo = (UserVo) o;
        return age == userVo.age && Objects.equals(name, userVo.name) && Objects.equals(addr, userVo.addr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, addr, age);
    }
}
