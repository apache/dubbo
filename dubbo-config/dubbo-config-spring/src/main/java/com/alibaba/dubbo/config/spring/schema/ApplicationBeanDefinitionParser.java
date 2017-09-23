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
 * @Createdate: 2017年09月22日 10:34
 */
public class ApplicationBeanDefinitionParser extends ValueBeanDefinitionParser implements BeanDefinitionParser {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationBeanDefinitionParser.class);

    public ApplicationBeanDefinitionParser(Class<?> beanClass){
        this(beanClass,true);
    }


    public ApplicationBeanDefinitionParser(Class<?> beanClass, boolean required){
        this.beanClass = beanClass;
        this.required = required;
    }


    @Override
    protected void processValue(String value, String property, RootBeanDefinition beanDefinition, ParserContext parserContext, Class<?> type, Element element, ManagedMap parameters) {
        processApplicationConsumerValue(value, property, beanDefinition, parserContext, type);
    }
}
