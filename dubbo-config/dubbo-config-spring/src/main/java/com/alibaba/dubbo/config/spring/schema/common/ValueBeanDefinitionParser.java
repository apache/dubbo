package com.alibaba.dubbo.config.spring.schema.common;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * 有些配置文件的处理方式大致相同，把相同部分放到这里
 * @author: zhangyinyue
 * @Createdate: 2017年09月23日 11:51
 */
public abstract class ValueBeanDefinitionParser extends CommonBeanDefinitionParser{

    private static final Logger logger = LoggerFactory.getLogger(ValueBeanDefinitionParser.class);

    /**
     * 处理Annotation，Argument，Protocol的属性值
     * @param value
     * @param type
     * @param property
     * @param beanDefinition
     */
    protected void processAnnotationArgumentProtocolValue(String value, Class<?> type, String property, RootBeanDefinition beanDefinition){
        if (value != null) {
            value = value.trim();
            if (value.length() > 0) {
                Object reference;
                if (isPrimitive(type)) {
                    reference = processXsdDefault(property, value);
                } else {
                    reference = new RuntimeBeanReference(value);
                }
                beanDefinition.getPropertyValues().addPropertyValue(property, reference);
            }
        }
    }

    /**
     * 处理Application,Consumer的属性值
     * @param value
     * @param property
     * @param beanDefinition
     * @param parserContext
     * @param type
     */
    protected void processApplicationConsumerValue(String value, String property, RootBeanDefinition beanDefinition, ParserContext parserContext, Class<?> type){
        if (value != null) {
            value = value.trim();
            if (value.length() > 0) {
                if (matchRegistry1Ref(property, value)) {
                    processRegistry1Ref(beanDefinition, value);
                } else if (matchRegistryMultiRef(property, value)) {
                    parseMultiRef(REGISTRIES, value, beanDefinition, parserContext);
                } else {
                    Object reference;
                    if (isPrimitive(type)) {
                        reference = processXsdDefault(property,value);
                    } else if (matchMonitorRef(property, parserContext, value)) {
                        // 兼容旧版本配置
                        reference = convertMonitor(value);
                    }else {
                        reference = new RuntimeBeanReference(value);
                    }
                    beanDefinition.getPropertyValues().addPropertyValue(property, reference);
                }
            }
        }
    }

    /**
     * 需要用到子类的日志类
     * @return
     */
    protected Logger getLogger(){
        return logger;
    }

    /**
     * 处理Monitor,Registry的属性值
     * @param value
     * @param property
     * @param beanDefinition
     * @param parserContext
     * @param type
     * @param element
     * @param parameters
     */
    protected void processMonitorRegistryValue(String value, String property, RootBeanDefinition beanDefinition, ParserContext parserContext, Class<?> type, Element element, ManagedMap parameters){
        if (matchParametersRef(property)) {
            parseParameters(element.getChildNodes(), beanDefinition,parameters);
        } else {
            if (value != null) {
                value = value.trim();
                if (value.length() > 0) {
                    if (matchProtocolMultiRef(property, value)) {
                        parseMultiRef(PROTOCOLS, value, beanDefinition, parserContext);
                    } else {
                        Object reference;
                        if (isPrimitive(type)) {
                            reference = processXsdDefault(property, value);
                        } else if (matchProtocol1Ref(property, parserContext, value)) {
                            reference = processProtocol1Ref(element, getLogger(), value);
                        }  else {
                            reference = new RuntimeBeanReference(value);
                        }
                        beanDefinition.getPropertyValues().addPropertyValue(property, reference);
                    }
                }
            }
        }
    }

    /**
     * 处理Provider,Reference的属性值
     * @param value
     * @param property
     * @param beanDefinition
     * @param parserContext
     * @param type
     * @param element
     * @param parameters
     */
    protected void processProviderReferenceValue(String value, String property, RootBeanDefinition beanDefinition, ParserContext parserContext, Class<?> type, Element element, ManagedMap parameters){
        if (value != null) {
            value = value.trim();
            if (value.length() > 0) {
                if (matchRegistry1Ref(property, value)) {
                    processRegistry1Ref(beanDefinition, value);
                } else if (matchRegistryMultiRef(property, value)) {
                    parseMultiRef(REGISTRIES, value, beanDefinition, parserContext);
                } else if (matchProtocolMultiRef(property, value)) {
                    parseMultiRef(PROTOCOLS, value, beanDefinition, parserContext);
                } else {
                    Object reference;
                    if (isPrimitive(type)) {
                        reference = processXsdDefault(property, value);
                    } else if (matchProtocol1Ref(property, parserContext, value)) {
                        reference = processProtocol1Ref(element, getLogger(), value);
                    } else if (matchMonitorRef(property, parserContext, value)) {
                        // 兼容旧版本配置
                        reference = convertMonitor(value);
                    }else {
                        reference = new RuntimeBeanReference(value);
                    }
                    beanDefinition.getPropertyValues().addPropertyValue(property, reference);
                }
            }
        }
    }


}
