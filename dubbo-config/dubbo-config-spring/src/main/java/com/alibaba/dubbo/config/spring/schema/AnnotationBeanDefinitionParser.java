package com.alibaba.dubbo.config.spring.schema;

import com.alibaba.dubbo.config.spring.AnnotationBean;
import com.alibaba.dubbo.config.spring.beans.factory.annotation.ReferenceAnnotationBeanPostProcessor;
import com.alibaba.dubbo.config.spring.beans.factory.annotation.ServiceAnnotationBeanPostProcessor;
import com.alibaba.dubbo.config.spring.util.BeanRegistrar;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import static org.springframework.util.StringUtils.commaDelimitedListToStringArray;
import static org.springframework.util.StringUtils.trimArrayElements;

/**
 * {@link AnnotationBean} {@link BeanDefinitionParser}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see ServiceAnnotationBeanPostProcessor
 * @see ReferenceAnnotationBeanPostProcessor
 * @since 2.5.9
 */
public class AnnotationBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    /**
     * parse
     * <prev>
     * &lt;dubbo:annotation package="" /&gt;
     * </prev>
     *
     * @param element
     * @param parserContext
     * @param builder
     */
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {

        String packageToScan = element.getAttribute("package");

        String[] packagesToScan = trimArrayElements(commaDelimitedListToStringArray(packageToScan));

        builder.addConstructorArgValue(packagesToScan);

        builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

        // Registers ReferenceAnnotationBeanPostProcessor
        registerReferenceAnnotationBeanPostProcessor(parserContext.getRegistry());

    }

    protected boolean shouldGenerateIdAsFallback() {
        return true;
    }

    /**
     * Registers {@link ReferenceAnnotationBeanPostProcessor} into {@link BeanFactory}
     *
     * @param registry {@link BeanDefinitionRegistry}
     */
    private void registerReferenceAnnotationBeanPostProcessor(BeanDefinitionRegistry registry) {

        // Register @Reference Annotation Bean Processor
        BeanRegistrar.registerInfrastructureBean(registry,
                ReferenceAnnotationBeanPostProcessor.BEAN_NAME, ReferenceAnnotationBeanPostProcessor.class);

    }

    protected Class<?> getBeanClass(Element element) {
        return ServiceAnnotationBeanPostProcessor.class;
    }

}
