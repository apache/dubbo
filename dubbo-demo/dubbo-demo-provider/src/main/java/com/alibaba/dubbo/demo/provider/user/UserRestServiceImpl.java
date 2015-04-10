package com.alibaba.dubbo.demo.provider.user;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.alibaba.dubbo.demo.user.User;
import com.alibaba.dubbo.demo.user.UserRestService;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.protocol.rest.support.ContentType;

/**
 * @author morly
 */
public class UserRestServiceImpl implements UserRestService {

	private Map<Long, User> users = new HashMap<Long, User>();

    public User getUser(Long id) {
        if (RpcContext.getContext().getRequest(HttpServletRequest.class) != null) {
            System.out.println("Client IP address from RpcContext: " + RpcContext.getContext().getRequest(HttpServletRequest.class).getRemoteAddr());
        }
        if (RpcContext.getContext().getResponse(HttpServletResponse.class) != null) {
            System.out.println("Response object from RpcContext: " + RpcContext.getContext().getResponse(HttpServletResponse.class));
        }
        User user = users.get(id);
        if (user == null) {
        	user = new User(1L, "justfortest");
        }
		return user;
    }

    public Long registerUser(User user) {
    	users.put(user.getId(), user);
        return user.getId();
    }
}
