package com.alibaba.dubbo.demo.provider.user;

import java.util.concurrent.atomic.AtomicLong;

import javax.validation.constraints.Min;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.alibaba.dubbo.demo.user.User;
import com.alibaba.dubbo.demo.user.UserService;
import com.alibaba.dubbo.rpc.protocol.rest.support.ContentType;

@Path("user")
@Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
@Produces({ContentType.APPLICATION_JSON_UTF_8, ContentType.TEXT_XML_UTF_8})
public class UserServiceImpl2 implements UserService{

	private final AtomicLong idGen = new AtomicLong();

	@GET
    @Path("{id : \\d+}")
    public User getUser(@PathParam("id") @Min(1L) Long id) {
        return new User(id, "username" + id);
    }

	@GET
    @Path("register")
    public Long registerUser(User user) {
        return idGen.incrementAndGet();
    }

}
