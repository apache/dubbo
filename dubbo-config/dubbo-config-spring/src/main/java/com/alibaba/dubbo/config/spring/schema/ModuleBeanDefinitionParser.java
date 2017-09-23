package com.alibaba.dubbo.config.spring.schema;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.config.spring.schema.common.CommonBeanDefinitionParser;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 *
 * @author: zhangyinyue
 * @Createdate: 2017年09月22日 10:35
 */
public class ModuleBeanDefinitionParser extends CommonBeanDefinitionParser implements BeanDefinitionParser {

    private static final Logger logger = LoggerFactory.getLogger(ModuleBeanDefinitionParser.class);

    public ModuleBeanDefinitionParser(Class<?> beanClass){
        this(beanClass,true);
    }

    public ModuleBeanDefinitionParser(Class<?> beanClass, boolean required){
        this.beanClass = beanClass;
        this.required = required;
    }

    @Override
    protected void processValue(String value, String property, RootBeanDefinition beanDefinition, ParserContext parserContext, Class<?> type, Element element, ManagedMap parameters) {
        if (value != null) {
            value = value.trim();
            if (value.length() > 0) {
                if (matchRegistry1Ref(property, value)) {
                    processRegistry1Ref(beanDefinition, value);
                } else if (matchRegistryMultiRef(property, value)) {
                    parseMultiRef(REGISTRIES, value, beanDefinition, parserContext);
                } else if (matchProviderRef(property, value)) {
                    parseMultiRef(PROVIDERS, value, beanDefinition, parserContext);
                } else {
                    Object reference;
                    if (isPrimitive(type)) {
                        reference = processXsdDefault(property, value);
                    } else if (matchMonitorRef(property, parserContext, value)) {
                        // 兼容旧版本配置
                        reference = convertMonitor(value);
                    } else {
                        reference = new RuntimeBeanReference(value);
                    }
                    beanDefinition.getPropertyValues().addPropertyValue(property, reference);
                }
            }
        }
    }
}
