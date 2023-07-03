package org.apache.dubbo.spring.boot.listener;

import java.lang.instrument.Instrumentation;
import org.apache.dubbo.spring.boot.interceptor.RunnableOrCallableActivation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;

public class DubboCrossThreadAnnotationListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {
    private Logger logger = LoggerFactory.getLogger(DubboCrossThreadAnnotationListener.class);
    private Instrumentation instrumentation;

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent applicationEnvironmentPreparedEvent) {
        RunnableOrCallableActivation.install(this.instrumentation);
        logger.info("finished byte buddy installation.");
    }

    public DubboCrossThreadAnnotationListener(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    private DubboCrossThreadAnnotationListener() {

    }
}
