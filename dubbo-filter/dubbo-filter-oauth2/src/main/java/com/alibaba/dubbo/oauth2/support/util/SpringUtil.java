package com.alibaba.dubbo.oauth2.support.util;

import com.alibaba.dubbo.config.spring.extension.SpringExtensionFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author wuyu
 */
public class SpringUtil {

    @SuppressWarnings("unchecked")
    public static Set<ApplicationContext> getApplicationContexts() {
        Field contextsFiled = ReflectionUtils.findField(SpringExtensionFactory.class, "contexts");
        contextsFiled.setAccessible(true);
        return (Set<ApplicationContext>) ReflectionUtils.getField(contextsFiled, null);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getBean(Class<T> type, String name) {
        Set<ApplicationContext> contexts = getApplicationContexts();
        for (ApplicationContext context : contexts) {
            if (context.containsBean(name)) {
                T bean = (T) context.getBean(name);
                if (type.isInstance(bean)) {
                    return bean;
                }
            }
        }
        return null;
    }

    public static <T> T getBean(Class<T> type) {
        Set<ApplicationContext> contexts = getApplicationContexts();
        for (ApplicationContext context : contexts) {
            Set<String> beanNamesForType = getBeanNamesForType(type);
            if(beanNamesForType.size()>0){
                for(String beanName : beanNamesForType){
                    if(context.containsBean(beanName)){
                        return context.getBean(type);
                    }
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getBean(String name) {
        Set<ApplicationContext> contexts = getApplicationContexts();
        for (ApplicationContext context : contexts) {
           if(context.containsBean(name)){
               return (T) context.getBean(name);
           }
        }
        return null;
    }

    public static <T> Set<String> getBeanNamesForType(Class<T> type) {
        Set<ApplicationContext> contexts = getApplicationContexts();
        Set<String> beanNames = new HashSet<String>();
        for (ApplicationContext context : contexts) {
            String[] beanName = context.getBeanNamesForType(type);
            beanNames.addAll(Arrays.asList(beanName));
        }
        return beanNames;
    }

    public static <T> Set<T> getBeans(Class<T> type) {
        Set<T> beans = new HashSet<T>();
        for (String beanName : getBeanNamesForType(type)) {
            T bean = getBean(type, beanName);
            beans.add(bean);
        }
        return beans;
    }

    public static <T> ApplicationContext getApplicationContext(Class<T> type) {
        Set<ApplicationContext> contexts = getApplicationContexts();
        for (ApplicationContext context : contexts) {
            String[] beanNames = context.getBeanNamesForType(type);
            if (beanNames != null) {
                return context;
            }
        }
        return null;
    }

    public static String resolve(String value) {
        if (StringUtils.hasText(value)) {
            Set<ApplicationContext> applicationContexts = SpringUtil.getApplicationContexts();
            for (ApplicationContext applicationContext : applicationContexts) {
                if (applicationContext instanceof ConfigurableApplicationContext) {
                    return ((ConfigurableApplicationContext) applicationContext).getEnvironment().resolvePlaceholders(value);
                }
            }
        }
        return "";
    }

}
