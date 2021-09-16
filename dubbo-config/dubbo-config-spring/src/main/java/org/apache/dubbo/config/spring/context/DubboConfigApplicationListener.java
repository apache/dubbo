package org.apache.dubbo.config.spring.context;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.dubbo.config.spring.context.event.DubboConfigInitEvent;
import org.apache.dubbo.config.spring.util.DubboBeanUtils;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An ApplicationListener to load config beans
 */
public class DubboConfigApplicationListener implements ApplicationListener<DubboConfigInitEvent>, ApplicationContextAware {

    private final static Log logger = LogFactory.getLog(DubboConfigApplicationListener.class);

    private ApplicationContext applicationContext;

    private ModuleModel moduleModel;

    private AtomicBoolean initialized = new AtomicBoolean();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        this.moduleModel = DubboBeanUtils.getModuleModel(applicationContext);
    }

    @Override
    public void onApplicationEvent(DubboConfigInitEvent event) {
        // It's expected to be notify at org.springframework.context.support.AbstractApplicationContext.registerListeners(),
        // before loading non-lazy singleton beans. At this moment, all BeanFactoryPostProcessor have been processed,
        if (initialized.compareAndSet(false, true)) {
            initDubboConfigBeans();
        }
    }

    private void initDubboConfigBeans() {
        // load DubboConfigBeanInitializer to init config beans
        if (applicationContext.containsBean(DubboConfigBeanInitializer.BEAN_NAME)) {
            applicationContext.getBean(DubboConfigBeanInitializer.BEAN_NAME, DubboConfigBeanInitializer.class);
        } else {
            logger.warn("Bean '" + DubboConfigBeanInitializer.BEAN_NAME + "' was not found");
        }

        // All infrastructure config beans are loaded, initialize dubbo here
        moduleModel.getDeployer().prepare();
    }


}
