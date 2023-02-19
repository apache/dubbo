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

package org.apache.dubbo.common.constants;

/**
 * Constants of Deprecated Method Invocation Counter.
 */
public final class DeprecatedMethodInvocationCounterConstants {
    private DeprecatedMethodInvocationCounterConstants() {
        throw new UnsupportedOperationException("No instance of DeprecatedMethodInvocationCounterConstants for you! ");
    }

    public static final String ERROR_CODE = "0-99";

    public static final String POSSIBLE_CAUSE = "invocation of deprecated method";

    public static final String EXTENDED_MESSAGE = "";

    public static final String LOGGER_MESSAGE_PREFIX = "Deprecated method invoked. The method is ";
}
