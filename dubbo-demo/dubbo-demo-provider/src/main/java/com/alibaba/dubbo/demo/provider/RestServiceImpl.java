package com.alibaba.dubbo.demo.provider;

import com.alibaba.dubbo.demo.RestService;
import com.alibaba.dubbo.demo.User;
import com.alibaba.dubbo.rpc.protocol.rest.support.ContentType;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

/**
 * Created by wuyu on 2017/2/2.
 */
@Path("/user")
public class RestServiceImpl implements RestService {
    @Override
    @Path("/{id}")
    @GET
    @Produces(ContentType.APPLICATION_JSON_UTF_8)
    public User getById(@PathParam("id") String id) {
        return new User(id, "wuyu");
    }

}
