package org.apache.dubbo.demo;

import org.apache.dubbo.demo.impl.SpringmvcDemoServiceImpl;
import org.apache.dubbo.rpc.protocol.mvc.servlet.MvcDispatcherServlet;
import org.junit.jupiter.api.Test;

public class TestDispatcherServlet {
    @Test
    public void testDispatcherServlet() {

        MvcDispatcherServlet mvcDispatcherServlet = new MvcDispatcherServlet();
        mvcDispatcherServlet.handlerParse(new SpringmvcDemoServiceImpl());
    }
}
