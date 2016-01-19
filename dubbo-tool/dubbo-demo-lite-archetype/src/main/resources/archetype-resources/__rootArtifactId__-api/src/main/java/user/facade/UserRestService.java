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
import com.alibaba.dubbo.rpc.protocol.rest.support.ContentType;

import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * This interface acts as some kind of service broker for the original UserService
 * <p/>
 * Here we want to simulate the twitter/weibo rest api, e.g.
 * <p/>
 * http://localhost:8888/user/1.json
 * http://localhost:8888/user/1.xml
 *
 * @author lishen
 */

@Path("users")
@Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
@Produces({ContentType.APPLICATION_JSON_UTF_8, ContentType.TEXT_XML_UTF_8})
public interface UserRestService {

    @GET
    @Path("{id : ${symbol_escape}${symbol_escape}d+}")
    public User getUser(@Min(value = 1L, message = "User ID must be greater than 1") @PathParam("id") Long id/*, @Context HttpServletRequest request*/);

    @POST
    @Path("register")
    RegistrationResult registerUser(User user);
}
