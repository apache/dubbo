package com.alibaba.dubbo.config.spring.schema;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.config.spring.schema.common.ValueBeanDefinitionParser;

import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 *
 * @author: zhangyinyue
 * @Createdate: 2017年09月22日 10:37
 */
public class MonitorBeanDefinitionParser extends ValueBeanDefinitionParser implements BeanDefinitionParser {

    private static final Logger logger = LoggerFactory.getLogger(MonitorBeanDefinitionParser.class);

    public MonitorBeanDefinitionParser(Class<?> beanClass){
        this(beanClass,true);
    }

    public MonitorBeanDefinitionParser(Class<?> beanClass, boolean required){
        this.beanClass = beanClass;
        this.required = required;
    }

    @Override
    protected Logger getLogger(){
        return logger;
    }

    @Override
    protected void processValue(String value, String property, RootBeanDefinition beanDefinition, ParserContext parserContext, Class<?> type, Element element, ManagedMap parameters) {
        processMonitorRegistryValue(value, property, beanDefinition, parserContext, type, element, parameters);
    }
}
