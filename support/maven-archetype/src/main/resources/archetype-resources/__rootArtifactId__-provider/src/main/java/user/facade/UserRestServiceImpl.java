#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/**
 * Copyright 1999-2014 dangdang.com.
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
package ${package}.user.facade;

import ${package}.user.User;
import ${package}.user.UserService;
import com.alibaba.dubbo.rpc.RpcContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.Min;
import javax.ws.rs.PathParam;

/**
 * User: kangfoo
 * Date: 14-12-9
 * Time: 下午3:45
 */
public class UserRestServiceImpl implements UserRestService {

    private static final Logger logger = LoggerFactory.getLogger(UserRestServiceImpl.class);

    private UserService userService;

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public User getUser(@Min(value = 1L, message = "User ID must be greater than 1") @PathParam("id") Long id) {
        // test context injection
//        System.out.println("Client address from @Context injection: " + (request != null ? request.getRemoteAddr() : ""));
//        System.out.println("Client address from RpcContext: " + RpcContext.getContext().getRemoteAddressString());
        if (RpcContext.getContext().getRequest(HttpServletRequest.class) != null) {
            System.out.println("Client IP address from RpcContext: " + RpcContext.getContext().getRequest(HttpServletRequest.class).getRemoteAddr());
        }
        if (RpcContext.getContext().getResponse(HttpServletResponse.class) != null) {
            System.out.println("Response object from RpcContext: " + RpcContext.getContext().getResponse(HttpServletResponse.class));
        }
        return userService.getUser(id);
    }

    @Override
    public RegistrationResult registerUser(User user) {
        return new RegistrationResult(userService.registerUser(user));
    }

}
