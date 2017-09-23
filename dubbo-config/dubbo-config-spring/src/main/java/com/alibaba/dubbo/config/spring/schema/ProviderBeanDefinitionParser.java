package com.alibaba.dubbo.config.spring.schema;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.config.spring.ServiceBean;
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
public class ProviderBeanDefinitionParser extends ValueBeanDefinitionParser implements BeanDefinitionParser {

    private static final Logger logger = LoggerFactory.getLogger(ProviderBeanDefinitionParser.class);

    public ProviderBeanDefinitionParser(Class<?> beanClass){
        this(beanClass,true);
    }

    public ProviderBeanDefinitionParser(Class<?> beanClass, boolean required){
        this.beanClass = beanClass;
        this.required = required;
    }


    @Override
    protected void dealWithBeanClass(ParserContext parserContext, String id, RootBeanDefinition beanDefinition, Element element) {
        parseNested(element, parserContext, ServiceBean.class, true, "service", "provider", id, beanDefinition);
    }

    @Override
    protected Logger getLogger(){
        return logger;
    }

    @Override
    protected void processValue(String value, String property, RootBeanDefinition beanDefinition, ParserContext parserContext, Class<?> type, Element element, ManagedMap parameters) {
            if (matchParametersRef(property)) {
                parseParameters(element.getChildNodes(), beanDefinition,parameters);
            } else {
                processProviderReferenceValue(value, property, beanDefinition, parserContext, type, element, parameters);
            }
    }


}
