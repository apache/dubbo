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
 * @Createdate: 2017年09月22日 10:35
 */
public class ArgumentBeanDefinitionParser extends ValueBeanDefinitionParser implements BeanDefinitionParser {

    private static final Logger logger = LoggerFactory.getLogger(ArgumentBeanDefinitionParser.class);

    public ArgumentBeanDefinitionParser(Class<?> beanClass){
        this(beanClass,true);
    }

    public ArgumentBeanDefinitionParser(Class<?> beanClass, boolean required){
        this.beanClass = beanClass;
        this.required = required;
    }


    @Override
    protected void processValue(String value, String property, RootBeanDefinition beanDefinition, ParserContext parserContext, Class<?> type, Element element, ManagedMap parameters) {
        processAnnotationArgumentProtocolValue(value,type,property,beanDefinition);
    }
}
