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

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.NoSuchElementException;

import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_NETWORK_IGNORED_INTERFACE;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NetUtilsInterfaceDisplayNameHasMetaCharactersTest {
    private static final String IGNORED_DISPLAY_NAME_HAS_METACHARACTERS = "Mock(R) ^$*+?.|-[0-9] Adapter";
    private static final String SELECTED_DISPLAY_NAME = "Selected Adapter";
    private static final String SELECTED_HOST_ADDR = "192.168.0.1";

    @Test
    public void testIgnoreGivenInterfaceNameWithMetaCharacters() throws Exception {
        String originIgnoredInterfaces = this.getIgnoredInterfaces();
        // mock static methods of final class NetworkInterface
        try (MockedStatic<NetworkInterface> mockedStaticNetif = Mockito.mockStatic(NetworkInterface.class)) {
            NetworkInterface mockIgnoredNetif = Mockito.mock(NetworkInterface.class);
            NetworkInterface mockSelectedNetif = Mockito.mock(NetworkInterface.class);
            NetworkInterface[] mockNetifs = { mockIgnoredNetif, mockSelectedNetif };
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

            InetAddress mockSelectedAddr = Mockito.mock(InetAddress.class);
            InetAddress[] mockAddrs = { mockSelectedAddr };
            Enumeration<InetAddress> mockEnumAddrs = new Enumeration<InetAddress>() {
                private int i = 0;
                public InetAddress nextElement() {
                    if (mockAddrs != null && i < mockAddrs.length) {
                        InetAddress addr = mockAddrs[i++];
                        return addr;
                    } else {
                        throw new NoSuchElementException();
                    }
                }

                public boolean hasMoreElements() {
                    return (mockAddrs != null && i < mockAddrs.length);
                }
            };

            // mock static method getNetworkInterfaces
            mockedStaticNetif.when(() -> { NetworkInterface.getNetworkInterfaces(); }).thenReturn(mockEnumIfs);

            Mockito.when(mockIgnoredNetif.isUp()).thenReturn(true);
            Mockito.when(mockIgnoredNetif.getDisplayName()).thenReturn(IGNORED_DISPLAY_NAME_HAS_METACHARACTERS);

            Mockito.when(mockSelectedNetif.isUp()).thenReturn(true);
            Mockito.when(mockSelectedNetif.getDisplayName()).thenReturn(SELECTED_DISPLAY_NAME);
            Mockito.when(mockSelectedNetif.getInetAddresses()).thenReturn(mockEnumAddrs);

            Mockito.when(mockSelectedAddr.isLoopbackAddress()).thenReturn(false);
            Mockito.when(mockSelectedAddr.getHostAddress()).thenReturn(SELECTED_HOST_ADDR);
            Mockito.when(mockSelectedAddr.isReachable(Mockito.anyInt())).thenReturn(true);

            this.setIgnoredInterfaces(IGNORED_DISPLAY_NAME_HAS_METACHARACTERS);
            NetworkInterface newNetworkInterface = NetUtils.findNetworkInterface();
            assertTrue(!IGNORED_DISPLAY_NAME_HAS_METACHARACTERS.equals(newNetworkInterface.getDisplayName()));
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
