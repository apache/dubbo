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
package org.apache.dubbo.common.utils;

import java.net.Socket;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultSerializeClassCheckerTest {

    @BeforeEach
    void setUp() {
        FrameworkModel.destroyAll();
    }

    @AfterEach
    void tearDown() {
        FrameworkModel.destroyAll();
    }

    @Test
    void testCommon() throws ClassNotFoundException {
        FrameworkModel.defaultModel().getBeanFactory().getBean(SerializeSecurityManager.class).setCheckStatus(SerializeCheckStatus.WARN);
        DefaultSerializeClassChecker defaultSerializeClassChecker = DefaultSerializeClassChecker.getInstance();

        for (int i = 0; i < 10; i++) {
            defaultSerializeClassChecker.loadClass(Thread.currentThread().getContextClassLoader(), ReentrantReadWriteLock.ReadLock.class.getName());
            defaultSerializeClassChecker.loadClass(Thread.currentThread().getContextClassLoader(), LinkedList.class.getName());
            defaultSerializeClassChecker.loadClass(Thread.currentThread().getContextClassLoader(), Integer.class.getName());
            defaultSerializeClassChecker.loadClass(Thread.currentThread().getContextClassLoader(), int.class.getName());
        }

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            defaultSerializeClassChecker.loadClass(Thread.currentThread().getContextClassLoader(), Socket.class.getName());
        });
        Assertions.assertTrue(FrameworkModel.defaultModel().getBeanFactory().getBean(SerializeSecurityManager.class)
            .getWarnedClasses().contains(Socket.class.getName()));
    }

    @Test
    void testAddAllow() throws ClassNotFoundException {
        System.setProperty(CommonConstants.CLASS_DESERIALIZE_ALLOWED_LIST, ReentrantReadWriteLock.WriteLock.class.getName() + "," + ReentrantReadWriteLock.ReadLock.class.getName());

        DefaultSerializeClassChecker defaultSerializeClassChecker = DefaultSerializeClassChecker.getInstance();
        for (int i = 0; i < 10; i++) {
            defaultSerializeClassChecker.loadClass(Thread.currentThread().getContextClassLoader(), ReentrantReadWriteLock.WriteLock.class.getName());
            defaultSerializeClassChecker.loadClass(Thread.currentThread().getContextClassLoader(), ReentrantReadWriteLock.ReadLock.class.getName());
        }

        System.clearProperty(CommonConstants.CLASS_DESERIALIZE_ALLOWED_LIST);
    }

    @Test
    void testAddBlock() {
        System.setProperty(CommonConstants.CLASS_DESERIALIZE_BLOCKED_LIST, Runtime.class.getName() + "," + Thread.class.getName());

        DefaultSerializeClassChecker defaultSerializeClassChecker = DefaultSerializeClassChecker.getInstance();
        for (int i = 0; i < 10; i++) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                defaultSerializeClassChecker.loadClass(Thread.currentThread().getContextClassLoader(), Runtime.class.getName());
            });
            Assertions.assertTrue(FrameworkModel.defaultModel().getBeanFactory().getBean(SerializeSecurityManager.class)
                .getWarnedClasses().contains(Runtime.class.getName()));
            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                defaultSerializeClassChecker.loadClass(Thread.currentThread().getContextClassLoader(), Thread.class.getName());
            });
            Assertions.assertTrue(FrameworkModel.defaultModel().getBeanFactory().getBean(SerializeSecurityManager.class)
                .getWarnedClasses().contains(Thread.class.getName()));
        }

        System.clearProperty(CommonConstants.CLASS_DESERIALIZE_BLOCKED_LIST);
    }

    @Test
    void testBlockAll() throws ClassNotFoundException {
        System.setProperty(CommonConstants.CLASS_DESERIALIZE_BLOCK_ALL, "true");
        System.setProperty(CommonConstants.CLASS_DESERIALIZE_ALLOWED_LIST, ReentrantReadWriteLock.WriteLock.class.getName());

        DefaultSerializeClassChecker defaultSerializeClassChecker = DefaultSerializeClassChecker.getInstance();
        for (int i = 0; i < 10; i++) {
            defaultSerializeClassChecker.loadClass(Thread.currentThread().getContextClassLoader(), ReentrantReadWriteLock.WriteLock.class.getName());
            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                defaultSerializeClassChecker.loadClass(Thread.currentThread().getContextClassLoader(), ReentrantReadWriteLock.ReadLock.class.getName());
            });
            Assertions.assertTrue(FrameworkModel.defaultModel().getBeanFactory().getBean(SerializeSecurityManager.class)
                .getWarnedClasses().contains(ReentrantReadWriteLock.ReadLock.class.getName()));
        }

        System.clearProperty(CommonConstants.CLASS_DESERIALIZE_BLOCK_ALL);
        System.clearProperty(CommonConstants.CLASS_DESERIALIZE_ALLOWED_LIST);
    }

    @Test
    void testStatus() throws ClassNotFoundException {
        FrameworkModel frameworkModel = FrameworkModel.defaultModel();
        SerializeSecurityManager ssm = frameworkModel.getBeanFactory().getBean(SerializeSecurityManager.class);
        ssm.setCheckStatus(SerializeCheckStatus.STRICT);

        DefaultSerializeClassChecker defaultSerializeClassChecker = DefaultSerializeClassChecker.getInstance();
        Assertions.assertEquals(Integer.class, defaultSerializeClassChecker.loadClass(Thread.currentThread().getContextClassLoader(), Integer.class.getName()));
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            defaultSerializeClassChecker.loadClass(Thread.currentThread().getContextClassLoader(), ReentrantReadWriteLock.class.getName());
        });
        Assertions.assertTrue(FrameworkModel.defaultModel().getBeanFactory().getBean(SerializeSecurityManager.class)
            .getWarnedClasses().contains(ReentrantReadWriteLock.class.getName()));

        ssm.setCheckStatus(SerializeCheckStatus.WARN);
        Assertions.assertEquals(ReentrantReadWriteLock.class, defaultSerializeClassChecker.loadClass(Thread.currentThread().getContextClassLoader(), ReentrantReadWriteLock.class.getName()));

        ssm.setCheckStatus(SerializeCheckStatus.DISABLE);
        Assertions.assertEquals(ReentrantReadWriteLock.class, defaultSerializeClassChecker.loadClass(Thread.currentThread().getContextClassLoader(), ReentrantReadWriteLock.class.getName()));
    }
}
