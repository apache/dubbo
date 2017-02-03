package com.alibaba.dubbo.demo;

import com.alibaba.dubbo.rpc.protocol.rest.support.ContentType;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

/**
 * Created by wuyu on 2017/2/2.
 */
@Path(value = "/user")
public interface RestService {

    @Path("/{id}")
    @GET
    @Produces(ContentType.APPLICATION_JSON_UTF_8)
    public User getById(@PathParam("id") String id);

}
