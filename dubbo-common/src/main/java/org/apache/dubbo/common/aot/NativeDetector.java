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
package org.apache.dubbo.common.aot;

import org.apache.dubbo.common.utils.SystemPropertyConfigUtils;

import static org.apache.dubbo.common.constants.CommonConstants.ThirdPartyProperty.GRAALVM_NATIVEIMAGE_IMAGECODE;

public abstract class NativeDetector {

    /**
     * See https://github.com/oracle/graal/blob/master/sdk/src/org.graalvm.nativeimage/src/org/graalvm/nativeimage/ImageInfo.java
     */
    private static final boolean IMAGE_CODE =
            (SystemPropertyConfigUtils.getSystemProperty(GRAALVM_NATIVEIMAGE_IMAGECODE) != null);

    /**
     * Returns {@code true} if invoked in the context of image building or during image runtime, else {@code false}.
     */
    public static boolean inNativeImage() {
        return IMAGE_CODE;
    }
}
