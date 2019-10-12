package org.apache.dubbo.config.spring.extension;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.GraceFulShutDown;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Set;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * @description
 * @sine 2.7.3
 */
public class GraceFulShutDownListenerTest {


    private static final Logger logger = LoggerFactory.getLogger(SpringExtensionFactory.class);
    private SpringExtensionFactory springExtensionFactory = new SpringExtensionFactory();
    private AnnotationConfigApplicationContext context1;
    private AnnotationConfigApplicationContext context2;
    @Test
    public void testGraceFulShutDown() {
        Set<ApplicationContext> contexts = SpringExtensionFactory.getContexts();
        for (ApplicationContext context : contexts){
            if (context instanceof ConfigurableApplicationContext) {
                ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) context;
                boolean active = configurableApplicationContext.isActive();
                if (active) {
                    String[] beanNames = context.getBeanNamesForType(GraceFulShutDown.class);
                    try {
                        for (String beanName : beanNames) {
                            GraceFulShutDown bean = context.getBean(beanName, GraceFulShutDown.class);
                            if (null != bean) {
                                bean.afterRegistriesDestroyed();
                                bean.afterProtocolDestroyed();
                            }
                        }
                    } catch (Exception e) {
                        Assertions.fail();
                    }

                }
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
