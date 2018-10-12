package org.apache.dubbo.config.spring.initializer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class DubboContextListener implements ServletContextListener {
    private static final Log logger = LogFactory.getLog(DubboContextListener.class);

    private ServletContextListener springContextListener;

    public DubboContextListener() {
        try {
            Class c = Class.forName("org.springframework.web.context.ContextLoaderListener");
            springContextListener = (ServletContextListener) c.newInstance();
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            logger.warn("Servlet container detects dubbo's web fragment configuration, and tries to load " +
                    "org.springframework.web.context.ContextLoaderListener but fails to find the class. " +
                    "If the application don't rely on Spring framework, pls. simply ignore");
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        if (springContextListener != null) {
            springContextListener.contextInitialized(servletContextEvent);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        if (springContextListener != null) {
            springContextListener.contextDestroyed(servletContextEvent);
        }
    }
}
