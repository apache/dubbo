package com.alibaba.dubbo.config.spring.beans.factory.annotation;

import com.alibaba.dubbo.config.ProtocolConfig;
import com.alibaba.dubbo.config.ProviderConfig;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.dubbo.config.spring.ServiceBean;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.alibaba.dubbo.config.spring.util.BeanFactoryUtils.getBeans;
import static com.alibaba.dubbo.config.spring.util.BeanFactoryUtils.getOptionalBean;

/**
 * {@link ServiceBean} Builder
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 2.5.7
 */
public class ServiceBeanBuilder extends AbstractAnnotationConfigBeanBuilder<Service, ServiceBean> {


    private ServiceBeanBuilder(Service annotation, ClassLoader classLoader, ApplicationContext applicationContext) {
        super(annotation, classLoader, applicationContext);
    }

    @Override
    protected ServiceBean<Object> doBuild() {
        return new ServiceBean<Object>(annotation);
    }

    @Override
    protected void preConfigureBean(Service annotation, ServiceBean bean) {
        Assert.notNull(bean, "@Service Bean must be set!");
    }

    @Override
    protected String resolveModuleConfigBeanName(Service service) {
        return service.module();
    }

    @Override
    protected String resolveApplicationConfigBeanName(Service service) {
        return service.application();
    }

    @Override
    protected String[] resolveRegistryConfigBeanNames(Service service) {
        return service.registry();
    }

    @Override
    protected String resolveMonitorConfigBeanName(Service service) {
        return service.monitor();
    }

    @Override
    protected void postConfigureBean(Service service, ServiceBean serviceBean) throws Exception {

        configureRef(service, serviceBean);

        configureInterface(service, serviceBean);

        configureApplicationContext(serviceBean);

        configureProviderConfigBean(service, serviceBean);

        configureProtocolConfigBeans(service, serviceBean);

        serviceBean.afterPropertiesSet();

    }

    private void configureRef(Service service, ServiceBean serviceBean) {
        serviceBean.setRef(bean);
    }

    private void configureInterface(Service service, ServiceBean serviceBean) {

        Class<?> interfaceClass = service.interfaceClass();

        if (void.class.equals(interfaceClass)) {

            interfaceClass = null;

            String interfaceClassName = service.interfaceName();

            if (StringUtils.hasText(interfaceClassName)) {
                if (ClassUtils.isPresent(interfaceClassName, classLoader)) {
                    interfaceClass = ClassUtils.resolveClassName(interfaceClassName, classLoader);
                }
            }

        }

        if (interfaceClass == null) {

            Class<?>[] allInterfaces = ClassUtils.getAllInterfaces(bean);

            if (allInterfaces.length > 0) {
                interfaceClass = allInterfaces[0];
            }

        }

        Assert.notNull(interfaceClass,
                "@Service interfaceClass() or interfaceName() or interface class must be present!");

        Assert.isTrue(interfaceClass.isInterface(),
                "The type that was annotated @Service is not an interface!");

        serviceBean.setInterface(interfaceClass);

    }


    private void configureApplicationContext(ServiceBean serviceBean) {
        serviceBean.setApplicationContext(applicationContext);
    }

    private void configureProviderConfigBean(Service service, ServiceBean serviceBean) {

        String provider = service.provider();

        ProviderConfig providerConfig = getOptionalBean(applicationContext, provider, ProviderConfig.class);

        serviceBean.setProvider(providerConfig);

    }


    private void configureProtocolConfigBeans(Service service, ServiceBean serviceBean) {

        String[] protocols = service.protocol();

        List<ProtocolConfig> protocolConfigs = getBeans(applicationContext, protocols, ProtocolConfig.class);

        serviceBean.setProtocols(protocolConfigs);

    }

    public static ServiceBeanBuilder create(Service annotation, ClassLoader classLoader,
                                            ApplicationContext applicationContext) {
        return new ServiceBeanBuilder(annotation, classLoader, applicationContext);
    }


}
