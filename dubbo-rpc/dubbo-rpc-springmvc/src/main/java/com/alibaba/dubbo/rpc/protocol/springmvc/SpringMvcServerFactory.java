package com.alibaba.dubbo.rpc.protocol.springmvc;

import com.alibaba.dubbo.remoting.http.HttpBinder;

/**
 * @author wuyu DATA:2016-6-8
 */
public class SpringMvcServerFactory {

    private HttpBinder httpBinder;

    public void setHttpBinder(HttpBinder httpBinder) {
        this.httpBinder = httpBinder;
    }

    public SpringMvcHttpServer createServer(String name) {
        if ("servlet".equalsIgnoreCase(name) || "jetty".equalsIgnoreCase(name) || "jetty9".equalsIgnoreCase(name) || "tomcat".equalsIgnoreCase(name)) {
            return new SpringMvcHttpServer(httpBinder);
        }

        throw new IllegalArgumentException(
                "Unrecognized server name: " + name );
    }

}
