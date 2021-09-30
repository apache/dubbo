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

package org.apache.dubbo.common.extension.activate.impl;

import org.apache.dubbo.common.extension.activate.ActivateWrapperExt1;


public class ActivateWrapperExt1Wrapper implements ActivateWrapperExt1 {
    private ActivateWrapperExt1 activateWrapperExt1;

    public ActivateWrapperExt1Wrapper(ActivateWrapperExt1 activateWrapperExt1) {
        this.activateWrapperExt1 = activateWrapperExt1;
    }

    @Override
    public String echo(String msg) {
        return activateWrapperExt1.echo(msg);
    }
}
