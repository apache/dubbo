package org.apache.dubbo.config.spring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.config.*;
import org.apache.dubbo.config.spi.ServiceExporter;
import org.apache.dubbo.config.spi.ServiceImporter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Created by lvxiang@ganji.com 2018/12/10 16:56
 *
 * @author <a href="mailto:lvxiang@ganji.com">simple</a>
 */
public class DubboBeanProcessor implements BeanPostProcessor {
    protected final Log logger = LogFactory.getLog(getClass());

    private  final ServiceExporter serviceExporter = ExtensionLoader.getExtensionLoader(ServiceExporter.class).getDefaultExtension();
    private  final ServiceImporter serviceImporter = ExtensionLoader.getExtensionLoader(ServiceImporter.class).getDefaultExtension();
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if ( !bean.getClass().getPackage().getName().startsWith("org.apache.dubbo.config")) {
            logger.info("not dubbo bean ="+bean);
            return bean;
        }

        //bean instanceof ServiceBean 或者  ServiceConfig.class.isAssignableFrom(bean.getClass())
        if (ServiceConfig.class.isAssignableFrom(bean.getClass())) {
            logger.info("spring bean 初始化完成,需要暴露,name:" + beanName + " ,bean=" + bean);
            serviceExporter.doExport(beanName,bean);
        } else if (ReferenceConfig.class.isAssignableFrom(bean.getClass())) {
            logger.info("spring bean 初始化完成,需要引入远程服务,name:" + beanName + " ,bean=" + bean);
            serviceImporter.doImport(beanName,bean);
        }
        return bean;
    }

}
