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
package com.alibaba.dubbo.callback.implicit.test;

import java.io.Serializable;

public class Person implements Serializable{
        private static final long serialVersionUID = 1L;
        public Person(int id, String name, int age) {
            this.id = id;
            this.name = name;
            this.age = age;
        }
        private int id;
        private String name ;
        private int age;
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
        public int getId() {
            return id;
        }
        public void setId(int id) {
            this.id = id;
        }
        @Override
        public String toString() {
            return "Person [name=" + name + ", age=" + age + "]";
        }
    }