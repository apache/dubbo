package com.alibaba.dubbo.demo;

import com.alibaba.fastjson.JSONObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * Created by wuyu on 2017/1/15.
 */
@Path("/comment")
public interface CommentService {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{name}")
    public JSONObject sayHello(@PathParam("name") String name);
}
