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
package org.apache.dubbo.common.serialize.hessian2;

import java.io.Serializable;

/**
 * Same shape as {@link KotlinStyleObject} but without {@code kotlin.Metadata}: an ordinary
 * hand-written Java singleton. Used to prove the Kotlin handling does not change how these
 * deserialize, since silently returning the singleton here would be a behaviour change.
 */
public final class JavaStyleSingleton implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final JavaStyleSingleton INSTANCE = new JavaStyleSingleton();

    private JavaStyleSingleton() {}
}
