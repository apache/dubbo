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
 * DemoService
 * 
 * @author william.liangf
 */
public interface DemoService extends MapService<Map<Object, Object>> {
    
    List<String> getEnvironment();

    List<String> getScene();

    Welcome sayHello(User user);
    
    String sayOK();

    void say();
    
    String sayName(String name);
    
    String sayParams(String name, String parentId, Role[] roles);
    
    long sayLong(long id);
    
    float sayFloat(float id);
    
    short sayShort(short id);
    
    Role sayRole(Role role);
    
    Role[] sayRoles(Role[] role);
    
    List<User> sayList(List<User> users);
    
    Welcome sayTwoHello(User user, User user2);
    
    String sayTwoName(String name1, String name2);
    
    HashSet<String> sayTwoParam(String name1, HashSet<String> name2);
    
    String sayQuery(IndexQueryParameter param);
    
    Map<Object, Object> sayMap(Map<Object, Object> map);

    HashMap<Object, Object> sayHashMap(HashMap<Object, Object> map);
    
    LinkedHashMap<Object, Object> sayLinkedHashMap(LinkedHashMap<Object, Object> map);
    
    void throwException();
    
    List<Person> listPerson(Person person);
    
    Person showPerson(Person person);
    
    void subscribe(IDemoCallback callback,String arg1);
    
    void unsubscribe(IDemoCallback callback);

}