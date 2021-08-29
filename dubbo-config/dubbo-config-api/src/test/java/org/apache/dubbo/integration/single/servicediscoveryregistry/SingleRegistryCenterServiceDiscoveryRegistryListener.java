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
package org.apache.dubbo.integration.single.servicediscoveryregistry;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryServiceListener;

@Activate(group = CommonConstants.PROVIDER, order = 1000)
public class SingleRegistryCenterServiceDiscoveryRegistryListener implements RegistryServiceListener {

    /**
     * register is called or not
     */
    private boolean registerIsCalled = false;

    /**
     * unRegisterIsCalled is called or not
     */
    private boolean unRegisterIsCalled = false;

    /**
     * subscribe is called or not
     */
    private boolean subscribeIsCalled = false;

    /**
     * unSubscribe is called or not
     */
    private boolean unSubscribeIsCalled = false;

    @Override
    public void onRegister(URL url, Registry registry) {
        registerIsCalled = true;
    }

    @Override
    public void onUnregister(URL url, Registry registry) {
        unRegisterIsCalled = true;
    }

    @Override
    public void onSubscribe(URL url, Registry registry) {
        subscribeIsCalled = true;
    }

    @Override
    public void onUnsubscribe(URL url, Registry registry) {
        unSubscribeIsCalled = true;
    }

    /**
     * Return if the register has called.
     */
    public boolean isRegisterHasCalled() {
        return registerIsCalled;
    }

    /**
     * Return if the unRegister has called.
     */
    public boolean isUnRegisterHasCalled() {
        return unRegisterIsCalled;
    }

    /**
     * Return if the subscribe has called.
     */
    public boolean isSubscribeHasCalled() {
        return subscribeIsCalled;
    }

    /**
     * Return if the unSubscribe has called.
     */
    public boolean isUnSubscribeHasCalled() {
        return unSubscribeIsCalled;
    }

    public void setRegisterIsCalled(boolean registerIsCalled) {
        this.registerIsCalled = registerIsCalled;
    }

    public void setUnRegisterIsCalled(boolean unRegisterIsCalled) {
        this.unRegisterIsCalled = unRegisterIsCalled;
    }

    public void setSubscribeIsCalled(boolean subscribeIsCalled) {
        this.subscribeIsCalled = subscribeIsCalled;
    }

    public void setUnSubscribeIsCalled(boolean unSubscribeIsCalled) {
        this.unSubscribeIsCalled = unSubscribeIsCalled;
    }
}
