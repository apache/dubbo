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
package po;

public class TestPO {
    private String name;
    private String address;
    private int age;

    public TestPO(String name, String address, int age) {
        this.name = name;
        this.address = address;
        this.age = age;
    }

    public TestPO() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public static TestPO getInstance() {
        return new TestPO("dubbo", "hangzhou", 10);
    }

    @Override
    public String toString() {
        return "TestPO{" +
            "name='" + name + '\'' +
            ", address='" + address + '\'' +
            ", age=" + age +
            '}';
    }
}
