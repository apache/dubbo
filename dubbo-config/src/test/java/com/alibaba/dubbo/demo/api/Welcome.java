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

/**
 * Hello
 * 
 * @author william.liangf
 */
public class Welcome implements IWelcome, Serializable {

    private static final long serialVersionUID = 1L;

    private String salutatory;

    private User user;
    
    private User user2;

    public Welcome() {
    }

    public Welcome(String salutatory, User user) {
        super();
        this.salutatory = salutatory;
        this.user = user;
    }

    public Welcome(String salutatory, User user, User user2) {
        super();
        this.salutatory = salutatory;
        this.user = user;
        this.user2 = user2;
    }

    public String getSalutatory() {
        return salutatory;
    }

    public void setSalutatory(String salutatory) {
        this.salutatory = salutatory;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getUser2() {
        return user2;
    }

    public void setUser2(User user2) {
        this.user2 = user2;
    }

    @Override
    public String toString() {
        return "Welcome [salutatory=" + salutatory + ", user=" + user + ", user2=" + user2 + "]";
    }

}