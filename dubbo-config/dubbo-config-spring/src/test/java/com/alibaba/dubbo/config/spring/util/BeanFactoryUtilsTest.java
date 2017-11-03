package com.alibaba.dubbo.config.spring.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * {@link BeanFactoryUtils} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 2.5.7
 */
public class BeanFactoryUtilsTest {

    private AnnotationConfigApplicationContext applicationContext;

    @Before
    public void init() {
        applicationContext = new AnnotationConfigApplicationContext();
    }

    @Test
    public void testGetOptionalBean() {

        applicationContext.register(TestBean.class);

        applicationContext.refresh();

        TestBean testBean = BeanFactoryUtils.getOptionalBean(applicationContext, "testBean", TestBean.class);

        Assert.assertNotNull(testBean);

        Assert.assertEquals("Hello,World", testBean.getName());

    }

    @Test
    public void testGetOptionalBeanIfAbsent() {

        applicationContext.refresh();

        TestBean testBean = BeanFactoryUtils.getOptionalBean(applicationContext, "testBean", TestBean.class);

        Assert.assertNull(testBean);
    }

    @Test
    public void testGetBeans() {

        applicationContext.register(TestBean.class);

        applicationContext.refresh();

        List<TestBean> testBeans = BeanFactoryUtils.getBeans(applicationContext, new String[]{"testBean"}, TestBean.class);

        Assert.assertEquals(1, testBeans.size());

        Assert.assertEquals("Hello,World", testBeans.get(0).getName());

    }

    @Test
    public void testGetBeansIfAbsent() {

        applicationContext.refresh();

        List<TestBean> testBeans = BeanFactoryUtils.getBeans(applicationContext, new String[]{"testBean"}, TestBean.class);

        Assert.assertTrue(testBeans.isEmpty());

    }

    @Component("testBean")
    private static class TestBean {

        private String name = "Hello,World";

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
