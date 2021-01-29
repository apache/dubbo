package org.apache.dubbo.config.spring.context;

import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.bootstrap.DubboBootstrapStartStopListener;
import org.apache.dubbo.config.spring.context.event.DubboBootstrapStatedEvent;
import org.apache.dubbo.config.spring.context.event.DubboBootstrapStopedEvent;

import org.springframework.context.ApplicationContext;

/**
 * convcert Dubbo bootstrap event to spring environment.
 *
 * @author <a href="mailto:chenxilzx1@gmail.com">theonefx</a>
 * @scene 2.7.9
 */
public class DubboBootstrapStartStopListenerSpringAdapter implements DubboBootstrapStartStopListener {

    static ApplicationContext applicationContext;

    @Override
    public void onStart(DubboBootstrap bootstrap) {
        if (applicationContext != null) {
            applicationContext.publishEvent(new DubboBootstrapStatedEvent(bootstrap));
        }
    }

    @Override
    public void onStop(DubboBootstrap bootstrap) {
        if (applicationContext != null) {
            applicationContext.publishEvent(new DubboBootstrapStopedEvent(bootstrap));
        }
    }
}
