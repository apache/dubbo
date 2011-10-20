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
package com.alibaba.dubbo.demo.provider.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.dubbo.demo.api.DemoService;
import com.alibaba.dubbo.demo.api.IDemoCallback;
import com.alibaba.dubbo.demo.api.IndexQueryParameter;
import com.alibaba.dubbo.demo.api.Person;
import com.alibaba.dubbo.demo.api.Role;
import com.alibaba.dubbo.demo.api.User;
import com.alibaba.dubbo.demo.api.Welcome;
import com.alibaba.dubbo.performance.PerformanceUtils;
import com.alibaba.dubbo.rpc.RpcContext;

/**
 * DemoServiceImpl
 * 
 * @author william.liangf
 */
public class DemoServiceImpl2 implements DemoService {

    public Welcome sayHello(User user) {
    	System.out.println(">>>>>>DemoServiceImpl2: " + user);
    	return new Welcome("provider2: " + RpcContext.getContext().getLocalAddress(), user);
    }

    public void say() {
        System.out.println(">>>>>>>>>>>>>>>>>>>say()");
    }

    public String sayOK() {
        return "OK";
    }

    public String sayName(String name) {
        return "say:" + name;
    }

    
    public Welcome sayTwoHello(User user, User user2) {
        return new Welcome("Hello", user, user2);
    }

    
    public String sayTwoName(String name1, String name2) {
        return name1 + " and " + name2;
    }

    
    public String sayQuery(IndexQueryParameter param) {
        return param.getParameters().toString();
    }

    
    public Role sayRole(Role role) {
        return role;
    }


    
    public Role[] sayRoles(Role[] roles) {
        return roles;
    }

	
	public Map<Object, Object> sayMap(Map<Object, Object> map) {
		map.put("xx", "yy");
		return map;
	}

	
	public void throwException() {
		throw new RuntimeException("OK");
	}

	
	public LinkedHashMap<Object, Object> sayLinkedHashMap(
			LinkedHashMap<Object, Object> map) {
		return map;
	}

	
	public HashMap<Object, Object> sayHashMap(HashMap<Object, Object> map) {
		return map;
	}

	
	public long sayLong(long id) {
		return id;
	}

	
	public short sayShort(short id) {
		return id;
	}

	
	public List<User> sayList(List<User> roles) {
		return roles;
	}

	
	public HashSet<String> sayTwoParam(String name1, HashSet<String> name2) {
		name2.add(name1);
		return name2;
	}

	public float sayFloat(float id) {
		return id;
	}
    
    public List<String> getEnvironment() {
        return PerformanceUtils.getEnvironment();
    }

    public List<String> getScene() {
        return PerformanceUtils.getScene();
    }

    public List<Person> listPerson(Person person) {
        List<Person> list = new ArrayList<Person>();
        for (int i = 0; i < 10; i ++) {
            list.add(person);
        }
        return list;
    }

    public Person showPerson(Person person) {
        return person;
    }
    
    public void subscribe(IDemoCallback callback,String arg1) {
    }

    public void unsubscribe(IDemoCallback callback) {
    }
    
    public String sayParams(String name, String parentId, Role[] roles) {
        return "Name:" + name + ", ParentId: " + parentId + ", Roles: " + Arrays.toString(roles);
    }
}