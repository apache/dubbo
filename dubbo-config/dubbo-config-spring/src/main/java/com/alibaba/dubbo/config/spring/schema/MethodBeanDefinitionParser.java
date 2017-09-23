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
public class MethodBeanDefinitionParser extends CommonBeanDefinitionParser implements BeanDefinitionParser {

    private static final Logger logger = LoggerFactory.getLogger(MethodBeanDefinitionParser.class);

    public MethodBeanDefinitionParser(Class<?> beanClass){
        this(beanClass,true);
    }

    public MethodBeanDefinitionParser(Class<?> beanClass, boolean required){
        this.beanClass = beanClass;
        this.required = required;
    }

    @Override
    protected void processValue(String value, String property, RootBeanDefinition beanDefinition, ParserContext parserContext, Class<?> type, Element element, ManagedMap parameters) {

        if (matchParametersRef(property)) {
            parseParameters(element.getChildNodes(), beanDefinition,parameters);
        } else if (ARGUMENTS.equals(property)) {
            String id = required ? String.valueOf(beanDefinition.getPropertyValues().get(ID)) : element.getAttribute(ID);
            parseArguments(id, element.getChildNodes(), beanDefinition, parserContext);
        } else {
            if (value != null) {
                value = value.trim();
                if (value.length() > 0) {
                    Object reference;
                    if (isPrimitive(type)) {
                        reference = processXsdDefault(property,value);
                    } else if (ONRETURN.equals(property)) {
                        int index = value.lastIndexOf(".");
                        String returnRef = value.substring(0, index);
                        String returnMethod = value.substring(index + 1);
                        reference = new RuntimeBeanReference(returnRef);
                        beanDefinition.getPropertyValues().addPropertyValue(ONRETURNMETHOD, returnMethod);
                    } else if (ONTHROW.equals(property)) {
                        int index = value.lastIndexOf(".");
                        String throwRef = value.substring(0, index);
                        String throwMethod = value.substring(index + 1);
                        reference = new RuntimeBeanReference(throwRef);
                        beanDefinition.getPropertyValues().addPropertyValue(ONTHROWMETHOD, throwMethod);
                    } else {
                        reference = new RuntimeBeanReference(value);
                    }
                    beanDefinition.getPropertyValues().addPropertyValue(property, reference);

                }
            }
        }
    }
}
