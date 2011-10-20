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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DemoServiceMock
 * 
 * @author william.liangf
 */
public class DemoServiceMock implements DemoService {

    public Welcome sayHello(User user) {
        System.out.println("-------------Mock");
        return null;
    }

    public String sayOK() {
        return "ERR";
    }

    public void say() {

    }

    public String sayName(String name) {
        return null;
    }

    public Role sayRole(Role role) {
        return null;
    }

    public Role[] sayRoles(Role[] role) {

        return null;
    }

    public List<User> sayList(List<User> role) {

        return null;
    }

    public Welcome sayTwoHello(User user, User user2) {

        return null;
    }

    public String sayTwoName(String name1, String name2) {

        return null;
    }

    public String sayQuery(IndexQueryParameter param) {

        return null;
    }

    public Map<Object, Object> sayMap(Map<Object, Object> map) {

        return null;
    }

    public HashMap<Object, Object> sayHashMap(HashMap<Object, Object> map) {

        return null;
    }

    public LinkedHashMap<Object, Object> sayLinkedHashMap(LinkedHashMap<Object, Object> map) {

        return null;
    }

    public void throwException() {

    }

    public long sayLong(long id) {

        return 0;
    }

    public short sayShort(short id) {

        return 0;
    }

    public HashSet<String> sayTwoParam(String name1, HashSet<String> name2) {
        return null;
    }

    public float sayFloat(float id) {

        return 0;
    }

    public List<String> getEnvironment() {

        return null;
    }

    public List<String> getScene() {

        return null;
    }

    public List<Person> listPerson(Person person) {

        return null;
    }

    public Person showPerson(Person person) {

        return null;
    }

    public void subscribe(IDemoCallback callback, String arg1) {

    }

    public void unsubscribe(IDemoCallback callback) {

    }

    public String sayParams(String name, String parentId, Role[] roles) {
        return null;
    }
}