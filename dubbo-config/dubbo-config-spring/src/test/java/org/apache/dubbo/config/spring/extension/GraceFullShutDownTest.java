package org.apache.dubbo.config.spring.extension;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * @author tiantian.yuan
 * @description
 * @date 2019-07-18 13:06
 */
public class GraceFullShutDownTest {


    private static final Logger logger = LoggerFactory.getLogger(SpringExtensionFactory.class);
    private SpringExtensionFactory springExtensionFactory = new SpringExtensionFactory();
    private AnnotationConfigApplicationContext context1;
    private AnnotationConfigApplicationContext context2;
    @Test
    public void testGraceFullShutDown(){
        Set<ApplicationContext> contexts = SpringExtensionFactory.getContexts();
        for (ApplicationContext context : contexts){
            GraceFullShutDown bean = null;
            try {
                 bean = context.getBean(GraceFullShutDown.class);
                if (null != bean){
                    bean.afterRegistriesDestroyed();
                    bean.afterProtocolDestroyed();
                }
            }catch (Exception e){
                Assertions.assertTrue(e instanceof NoSuchBeanDefinitionException);
            }


        }
    }
    @BeforeEach
    public void init() {
        context1 = new AnnotationConfigApplicationContext();
        context1.register(getClass());
        context1.refresh();
        context2 = new AnnotationConfigApplicationContext();
        context2.register(BeanForContext2.class);
        context2.refresh();
        SpringExtensionFactory.addApplicationContext(context1);
        SpringExtensionFactory.addApplicationContext(context2);
    }

}
