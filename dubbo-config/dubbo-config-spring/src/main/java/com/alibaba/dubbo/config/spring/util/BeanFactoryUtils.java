package com.alibaba.dubbo.config.spring.util;

import com.alibaba.dubbo.common.utils.StringUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;

import java.util.*;

import static org.springframework.beans.factory.BeanFactoryUtils.beanNamesForTypeIncludingAncestors;
import static org.springframework.beans.factory.BeanFactoryUtils.beanOfTypeIncludingAncestors;
import static org.springframework.beans.factory.BeanFactoryUtils.beansOfTypeIncludingAncestors;

/**
 * {@link BeanFactory} Utilities class
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see BeanFactory
 * @see ConfigurableBeanFactory
 * @see org.springframework.beans.factory.BeanFactoryUtils
 * @since 2.5.7
 */
public class BeanFactoryUtils {


    /**
     * Get optional Bean
     *
     * @param beanFactory {@link ListableBeanFactory}
     * @param beanName    the name of Bean
     * @param beanType    the {@link Class type} of Bean
     * @param <T>         the {@link Class type} of Bean
     * @return A bean if present , or <code>null</code>
     */
    public static <T> T getOptionalBean(ListableBeanFactory beanFactory, String beanName, Class<T> beanType) {

        String[] allBeanNames = beanNamesForTypeIncludingAncestors(beanFactory, beanType);

        if (!StringUtils.isContains(allBeanNames, beanName)) {
            return null;
        }

        Map<String, T> beansOfType = beansOfTypeIncludingAncestors(beanFactory, beanType);

        return beansOfType.get(beanName);

    }


    /**
     * Gets name-matched Beans from {@link ListableBeanFactory BeanFactory}
     *
     * @param beanFactory {@link ListableBeanFactory BeanFactory}
     * @param beanNames   the names of Bean
     * @param beanType    the {@link Class type} of Bean
     * @param <T>         the {@link Class type} of Bean
     * @return
     */
    public static <T> List<T> getBeans(ListableBeanFactory beanFactory, String[] beanNames, Class<T> beanType) {

        String[] allBeanNames = beanNamesForTypeIncludingAncestors(beanFactory, beanType);

        List<T> beans = new ArrayList<T>(beanNames.length);

        for (String beanName : beanNames) {
            if (StringUtils.isContains(allBeanNames, beanName)) {
                beans.add(beanOfTypeIncludingAncestors(beanFactory, beanType));
            }
        }

        return Collections.unmodifiableList(beans);

    }

}
