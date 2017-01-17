package com.alibaba.dubbo.demo.provider;

import com.alibaba.dubbo.demo.CommentService;
import com.alibaba.fastjson.JSONObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * Created by wuyu on 2017/1/15.
 */
@Path("/comment")
public class CommentServiceImpl implements CommentService {
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{name}")
    public JSONObject sayHello(@PathParam(value = "name") String name) {
        JSONObject json = new JSONObject();
        json.put("name", "Hello " + name);
        return json;
    }
}
