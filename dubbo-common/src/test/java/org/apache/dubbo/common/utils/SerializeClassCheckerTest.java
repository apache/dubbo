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

import org.apache.dubbo.common.constants.CommonConstants;

import javassist.compiler.Javac;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class SerializeClassCheckerTest {

    @BeforeEach
    public void setUp() {
        SerializeClassChecker.clearInstance();
    }

    @Test
    public void testCommon() {
        SerializeClassChecker serializeClassChecker = SerializeClassChecker.getInstance();

        for (int i = 0; i < 10; i++) {
            serializeClassChecker.validateClass(List.class.getName());
            serializeClassChecker.validateClass(LinkedList.class.getName());
            serializeClassChecker.validateClass(Integer.class.getName());
            serializeClassChecker.validateClass(int.class.getName());

            serializeClassChecker.validateClass(List.class.getName().toUpperCase(Locale.ROOT));
            serializeClassChecker.validateClass(LinkedList.class.getName().toUpperCase(Locale.ROOT));
            serializeClassChecker.validateClass(Integer.class.getName().toUpperCase(Locale.ROOT));
            serializeClassChecker.validateClass(int.class.getName().toUpperCase(Locale.ROOT));
        }

        Assertions.assertThrows(IllegalArgumentException.class, ()-> {
            serializeClassChecker.validateClass(Socket.class.getName());
        });
    }

    @Test
    public void testAddAllow() {
        System.setProperty(CommonConstants.CLASS_DESERIALIZE_ALLOWED_LIST, Socket.class.getName() + "," + Javac.class.getName());

        SerializeClassChecker serializeClassChecker = SerializeClassChecker.getInstance();
        for (int i = 0; i < 10; i++) {
            serializeClassChecker.validateClass(Socket.class.getName());
            serializeClassChecker.validateClass(Javac.class.getName());
        }

        System.clearProperty(CommonConstants.CLASS_DESERIALIZE_ALLOWED_LIST);
    }

    @Test
    public void testAddBlock() {
        System.setProperty(CommonConstants.CLASS_DESERIALIZE_BLOCKED_LIST, LinkedList.class.getName() + "," + Integer.class.getName());

        SerializeClassChecker serializeClassChecker = SerializeClassChecker.getInstance();
        for (int i = 0; i < 10; i++) {
            Assertions.assertThrows(IllegalArgumentException.class, ()-> {
                serializeClassChecker.validateClass(LinkedList.class.getName());
            });
            Assertions.assertThrows(IllegalArgumentException.class, ()-> {
                serializeClassChecker.validateClass(Integer.class.getName());
            });
        }

        System.clearProperty(CommonConstants.CLASS_DESERIALIZE_BLOCKED_LIST);
    }

    @Test
    public void testBlockAll() {
        System.setProperty(CommonConstants.CLASS_DESERIALIZE_BLOCK_ALL, "true");
        System.setProperty(CommonConstants.CLASS_DESERIALIZE_ALLOWED_LIST, LinkedList.class.getName());

        SerializeClassChecker serializeClassChecker = SerializeClassChecker.getInstance();
        for (int i = 0; i < 10; i++) {
            serializeClassChecker.validateClass(LinkedList.class.getName());
            Assertions.assertThrows(IllegalArgumentException.class, ()-> {
                serializeClassChecker.validateClass(Integer.class.getName());
            });
        }

        System.clearProperty(CommonConstants.CLASS_DESERIALIZE_BLOCK_ALL);
        System.clearProperty(CommonConstants.CLASS_DESERIALIZE_ALLOWED_LIST);
    }
}
