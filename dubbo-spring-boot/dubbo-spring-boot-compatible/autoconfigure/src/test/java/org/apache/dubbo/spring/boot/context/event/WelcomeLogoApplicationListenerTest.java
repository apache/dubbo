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
package org.apache.dubbo.spring.boot.context.event;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * {@link WelcomeLogoApplicationListener} Test
 *
 * @see WelcomeLogoApplicationListener
 * @since 2.7.0
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = {WelcomeLogoApplicationListener.class}
)
public class WelcomeLogoApplicationListenerTest {

    @Autowired
    private WelcomeLogoApplicationListener welcomeLogoApplicationListener;

    @Test
    public void testOnApplicationEvent() {

        Assert.assertNotNull(welcomeLogoApplicationListener.buildBannerText());

    }

}
