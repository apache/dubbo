#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
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
package ${package}.consumer;

import ${package}.user.User;
import ${package}.user.UserService;
import ${package}.user.facade.UserRestService;

/**
 * User: kangfoo
 * Date: 14-12-9
 * Time: 下午4:12
 */
public class DemoAction {

    private UserRestService userRestService;
    private UserService userService;

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setUserRestService(UserRestService userRestService) {
        this.userRestService = userRestService;
    }

    public void start() throws Exception {


        User user = new User(1L, "larrypage");
        System.out.println("SUCESS: registered user with id " + userRestService.registerUser(user).getId());

        System.out.println("SUCESS: got user " + userService.getUser(1L));
    }

}
