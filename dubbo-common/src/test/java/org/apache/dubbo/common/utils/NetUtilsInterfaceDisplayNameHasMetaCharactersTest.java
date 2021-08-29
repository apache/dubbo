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

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.net.NetworkInterface;

import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_NETWORK_IGNORED_INTERFACE;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NetUtilsInterfaceDisplayNameHasMetaCharactersTest {
    private static final String DISPLAY_NAME_HAS_METACHARACTERS = "Mock(R) ^$*+?.|-[0-9] Adapter";

    @Test
    public void testIgnoreGivenInterfaceNameWithMetaCharacters() throws Exception {        
        String originIgnoredInterfaces = this.getIgnoredInterfaces();
        // mock static method (getNetworkInterfaces) of final class (NetworkInterface)
        try (MockedStatic<NetworkInterface> mockedStaticNetif = Mockito.mockStatic(NetworkInterface.class)) {
            NetworkInterface mockNetif = Mockito.mock(NetworkInterface.class);
            NetworkInterface[] mockNetifs = { mockNetif };
            Enumeration<NetworkInterface> mockEnumIfs = new Enumeration<NetworkInterface>() {
                private int i = 0;
                public NetworkInterface nextElement() {
                    if (mockNetifs != null && i < mockNetifs.length) {
                        NetworkInterface netif = mockNetifs[i++];
                        return netif;
                    } else {
                        throw new NoSuchElementException();
                    }
                }

                public boolean hasMoreElements() {
                    return (mockNetifs != null && i < mockNetifs.length);
                }
            };
            mockedStaticNetif.when(() -> { NetworkInterface.getNetworkInterfaces(); }).thenReturn(mockEnumIfs);
            Mockito.when(mockNetif.isUp()).thenReturn(true);
            Mockito.when(mockNetif.getDisplayName()).thenReturn(DISPLAY_NAME_HAS_METACHARACTERS);

            this.setIgnoredInterfaces(DISPLAY_NAME_HAS_METACHARACTERS);
            NetworkInterface newNetworkInterface = NetUtils.findNetworkInterface();
            if(newNetworkInterface!=null){
                assertTrue(!DISPLAY_NAME_HAS_METACHARACTERS.equals(newNetworkInterface.getDisplayName()));
            }
        } finally {
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
