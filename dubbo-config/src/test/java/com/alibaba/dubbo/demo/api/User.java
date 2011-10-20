/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.demo.api;

import java.io.Serializable;
import java.util.Arrays;

/**
 * User
 * 
 * @author william.liangf
 */
public class User implements Serializable {

    private static final long serialVersionUID = 5840134024756337246L;

    private String name;

    private int age;
    
    private String[] phones;
    
    private Address address;
    
    private Role role;
    
    public User() {
    }

    public User(String name, int age, String[] phones, Address address, Role role) {
        super();
        this.name = name;
        this.age = age;
        this.phones = phones;
        this.address = address;
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String[] getPhones() {
        return phones;
    }

    public void setPhones(String[] phones) {
        this.phones = phones;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "User [address=" + address + ", age=" + age + ", name=" + name + ", phones="
                + Arrays.toString(phones) + "]" + ", role="
                + role + "]";
    }

}