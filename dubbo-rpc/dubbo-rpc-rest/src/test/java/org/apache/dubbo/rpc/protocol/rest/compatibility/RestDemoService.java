package org.apache.dubbo.rpc.protocol.rest.compatibility;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

@Path("rest")
public interface RestDemoService {
    @GET
    @Path("hello")
    @Consumes({MediaType.APPLICATION_JSON})
    String sayHello(String name);

    @GET
    @Path("hi")
    @Consumes({MediaType.APPLICATION_JSON})
    String sayHi();

    @GET
    @Path("fruit")
    @Consumes({MediaType.APPLICATION_JSON})
    Fruit sayFruit();

    @GET
    @Path("apple")
    @Consumes({MediaType.APPLICATION_JSON})
    Apple sayApple();
}
