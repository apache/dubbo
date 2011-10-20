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
public class DemoServiceStub implements DemoService {
    
    private DemoService demoService;

	public DemoServiceStub(DemoService demoService){
        this.demoService = demoService;
    }

    public Welcome sayHello(User user) {
        System.out.println("-----------------Stub");
        return demoService.sayHello(user);
    }

    public String sayOK() {
        return demoService.sayOK();
    }

    public void say() {
        demoService.say();
    }

    public String sayName(String name) {
        return demoService.sayName(name);
    }

    public long sayLong(long id) {
        return demoService.sayLong(id);
    }

    public float sayFloat(float id) {
        return demoService.sayFloat(id);
    }

    public short sayShort(short id) {
        return demoService.sayShort(id);
    }

    public Role sayRole(Role role) {
        return demoService.sayRole(role);
    }

    public Role[] sayRoles(Role[] role) {
        return demoService.sayRoles(role);
    }

    public List<User> sayList(List<User> role) {
        return demoService.sayList(role);
    }

    public Welcome sayTwoHello(User user, User user2) {
        return demoService.sayTwoHello(user, user2);
    }

    public String sayTwoName(String name1, String name2) {
        return demoService.sayTwoName(name1, name2);
    }

    public HashSet<String> sayTwoParam(String name1, HashSet<String> name2) {
        return demoService.sayTwoParam(name1, name2);
    }

    public String sayQuery(IndexQueryParameter param) {
        return demoService.sayQuery(param);
    }

    public Map<Object, Object> sayMap(Map<Object, Object> map) {
        return demoService.sayMap(map);
    }

    public HashMap<Object, Object> sayHashMap(HashMap<Object, Object> map) {
        return demoService.sayHashMap(map);
    }

    public LinkedHashMap<Object, Object> sayLinkedHashMap(LinkedHashMap<Object, Object> map) {
        return demoService.sayLinkedHashMap(map);
    }

    public void throwException() {
        demoService.throwException();
    }

    public List<String> getEnvironment() {
        return demoService.getEnvironment();
    }

    public List<String> getScene() {
        return demoService.getScene();
    }

    public List<Person> listPerson(Person person) {
        return demoService.listPerson(person);
    }

    public Person showPerson(Person person) {
        return demoService.showPerson(person);
    }

    public void subscribe(IDemoCallback callback,String arg1) {
    }

    public void unsubscribe(IDemoCallback callback) {
    }
    
    public String sayParams(String name, String parentId, Role[] roles) {
        return null;
    }
}