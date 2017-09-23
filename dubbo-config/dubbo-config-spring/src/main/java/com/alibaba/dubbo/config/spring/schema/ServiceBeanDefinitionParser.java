package com.alibaba.dubbo.config.spring.schema;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.alibaba.dubbo.config.spring.schema.common.CommonBeanDefinitionParser;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 *
 * @author: zhangyinyue
 * @Createdate: 2017年09月22日 10:38
 */
public class ServiceBeanDefinitionParser extends CommonBeanDefinitionParser implements BeanDefinitionParser {

    private static final Logger logger = LoggerFactory.getLogger(ServiceBeanDefinitionParser.class);

    public ServiceBeanDefinitionParser(Class<?> beanClass){
        this(beanClass,true);
    }

    public ServiceBeanDefinitionParser(Class<?> beanClass, boolean required){
        this.beanClass = beanClass;
        this.required = required;
    }


    @Override
    protected void dealWithBeanClass(ParserContext parserContext, String id, RootBeanDefinition beanDefinition, Element element) {
        String className = element.getAttribute("class");
        if (className != null && className.length() > 0) {
            RootBeanDefinition classDefinition = new RootBeanDefinition();
            classDefinition.setBeanClass(ReflectUtils.forName(className));
            classDefinition.setLazyInit(false);
            parseProperties(element.getChildNodes(), classDefinition);
            beanDefinition.getPropertyValues().addPropertyValue(REF, new BeanDefinitionHolder(classDefinition, id + "Impl"));
        }
    }

    @Override
    protected void processValue(String value, String property, RootBeanDefinition beanDefinition, ParserContext parserContext, Class<?> type, Element element, ManagedMap parameters) {

        if (matchParametersRef(property)) {
            parseParameters(element.getChildNodes(), beanDefinition,parameters);
        } else if (matchMethodsRef(property)) {
            String id = beanDefinition.getPropertyValues().get(ID).toString();
            parseMethods(id, element.getChildNodes(), beanDefinition, parserContext);
        } else {
            if (value != null) {
                value = value.trim();
                if (value.length() > 0) {
                    if (matchRegistry1Ref(property, value)) {
                        processRegistry1Ref(beanDefinition, value);
                    } else if (matchRegistryMultiRef(property, value)) {
                        parseMultiRef(REGISTRIES, value, beanDefinition, parserContext);
                    } else if (matchProviderRef(property, value)) {
                        parseMultiRef(PROVIDERS, value, beanDefinition, parserContext);
                    } else if (matchProtocolMultiRef(property, value)) {
                        parseMultiRef(PROTOCOLS, value, beanDefinition, parserContext);
                    } else {
                        Object reference;
                        if (isPrimitive(type)) {
                            reference = processXsdDefault(property, value);
                        } else if (matchProtocol1Ref(property, parserContext, value)) {
                            reference = processProtocol1Ref(element, logger, value);
                        } else if (matchMonitorRef(property, parserContext, value)) {
                            // 兼容旧版本配置
                            reference = convertMonitor(value);
                        } else {
                            if (REF.equals(property) && parserContext.getRegistry().containsBeanDefinition(value)) {
                                BeanDefinition refBean = parserContext.getRegistry().getBeanDefinition(value);
                                if (!refBean.isSingleton()) {
                                    throw new IllegalStateException("The exported service ref " + value + " must be singleton! Please set the " + value + " bean scope to singleton, eg: <bean id=\"" + value + "\" scope=\"singleton\" ...>");
                                }
                            }
                            reference = new RuntimeBeanReference(value);
                        }
                        beanDefinition.getPropertyValues().addPropertyValue(property, reference);
                    }
                }
            }
        }
    }


}
