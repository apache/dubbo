/*
 * Copyright 1999-2012 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.examples.annotation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.alibaba.dubbo.examples.annotation.action.AnnotationAction;

/**
 * AnnotationTest
 * 
 * @author william.liangf
 */
public class AnnotationTest {

    @Test
    public void testAnnotation() {
        ClassPathXmlApplicationContext providerContext = new ClassPathXmlApplicationContext(AnnotationTest.class.getPackage().getName().replace('.', '/') + "/annotation-provider.xml");
        providerContext.start();
        try {
            ClassPathXmlApplicationContext consumerContext = new ClassPathXmlApplicationContext(AnnotationTest.class.getPackage().getName().replace('.', '/') + "/annotation-consumer.xml");
            consumerContext.start();
            try {
                AnnotationAction annotationAction = (AnnotationAction) consumerContext.getBean("annotationAction");
                String hello = annotationAction.doSayHello("world");
                assertEquals("annotation: hello, world", hello);
            } finally {
                consumerContext.stop();
                consumerContext.close();
            }
        } finally {
            providerContext.stop();
            providerContext.close();
        }
    }

}
