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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import mockit.Expectations;
import mockit.Mocked;

import java.lang.reflect.Constructor;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.net.NetworkInterface;

import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_NETWORK_IGNORED_INTERFACE;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * use JMockit to mock static method (getNetworkInterfaces) of final class (NetworkInterface)
 * 1. set goal for single mvn test: test -Dtest=NetUtilsMockStaticMethodOfFinalClassTest
 * 2. set vm for junit: -javaagent:${LocalRepository-Path}/org/jmockit/jmockit/${Jmockit-Version}/jmockit-${Jmockit-Version}.jar 
 */
public class NetUtilsInterfaceNameHasMetaCharactersTest {
    @Mocked NetworkInterface mockNetworkInterface;

    private String mockDisplayNameWithMetaCharacters = "Mock(R) ^$*+?.|-[0-9] Adapter";
    private NetworkInterface[] mockInterfaces = new NetworkInterface[1];
    private Enumeration<NetworkInterface> mockEnumInterfaces;

    @BeforeEach
    public void setUp() throws Exception {
        Constructor<NetworkInterface> constructor = NetworkInterface.class.getDeclaredConstructor();
        constructor.setAccessible(true);        
        mockInterfaces[0] = (NetworkInterface) constructor.newInstance();
        mockEnumInterfaces = new Enumeration<NetworkInterface>() {
            private int i = 0;
            public NetworkInterface nextElement() {
                if (mockInterfaces != null && i < mockInterfaces.length) {
                    NetworkInterface netif = mockInterfaces[i++];
                    return netif;
                } else {
                    throw new NoSuchElementException();
                }
            }

            public boolean hasMoreElements() {
                return (mockInterfaces != null && i < mockInterfaces.length);
            }
        };

        new Expectations() {
            {
                NetworkInterface.getNetworkInterfaces();
                result = mockEnumInterfaces;

                mockNetworkInterface.isUp();
                result = true;
                mockNetworkInterface.getDisplayName();
                result = mockDisplayNameWithMetaCharacters;
            }
        };
    }

    @Test
    public void testIgnoreGivenInterfaceNameWithMetaCharacters() throws Exception {        
        String originIgnoredInterfaces = this.getIgnoredInterfaces();
        try{
            this.setIgnoredInterfaces(mockDisplayNameWithMetaCharacters);
            NetworkInterface newNetworkInterface = NetUtils.findNetworkInterface();
            if(newNetworkInterface!=null){
                assertTrue(!mockDisplayNameWithMetaCharacters.equals(newNetworkInterface.getDisplayName()));
            }
        }finally {
            // recover the origin ignored interfaces
            this.setIgnoredInterfaces(originIgnoredInterfaces);
        }
    }

    private String getIgnoredInterfaces(){
        return System.getProperty(DUBBO_NETWORK_IGNORED_INTERFACE);
    }

    private void setIgnoredInterfaces(String ignoredInterfaces){
        if(ignoredInterfaces!=null){
            System.setProperty(DUBBO_NETWORK_IGNORED_INTERFACE,ignoredInterfaces);
        }else{
            System.setProperty(DUBBO_NETWORK_IGNORED_INTERFACE,"");
        }
    }

}
