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
package org.apache.dubbo.rpc.model;

import java.lang.reflect.Method;

public class AsyncMethodInfo {
    // callback instance when async-call is invoked
    private Object oninvokeInstance;

    // callback method when async-call is invoked
    private Method oninvokeMethod;

    // callback instance when async-call is returned
    private Object onreturnInstance;

    // callback method when async-call is returned
    private Method onreturnMethod;

    // callback instance when async-call has exception thrown
    private Object onthrowInstance;

    // callback method when async-call has exception thrown
    private Method onthrowMethod;

    public Object getOninvokeInstance() {
        return oninvokeInstance;
    }

    public void setOninvokeInstance(Object oninvokeInstance) {
        this.oninvokeInstance = oninvokeInstance;
    }

    public Method getOninvokeMethod() {
        return oninvokeMethod;
    }

    public void setOninvokeMethod(Method oninvokeMethod) {
        this.oninvokeMethod = oninvokeMethod;
    }

    public Object getOnreturnInstance() {
        return onreturnInstance;
    }

    public void setOnreturnInstance(Object onreturnInstance) {
        this.onreturnInstance = onreturnInstance;
    }

    public Method getOnreturnMethod() {
        return onreturnMethod;
    }

    public void setOnreturnMethod(Method onreturnMethod) {
        this.onreturnMethod = onreturnMethod;
    }

    public Object getOnthrowInstance() {
        return onthrowInstance;
    }

    public void setOnthrowInstance(Object onthrowInstance) {
        this.onthrowInstance = onthrowInstance;
    }

    public Method getOnthrowMethod() {
        return onthrowMethod;
    }

    public void setOnthrowMethod(Method onthrowMethod) {
        this.onthrowMethod = onthrowMethod;
    }
}
