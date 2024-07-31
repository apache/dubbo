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
package org.apache.dubbo.rpc.protocol.tri.rest.support.spring;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.ArgumentConverter;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Activate(onClass = "org.springframework.util.MultiValueMap")
public class MultiValueMapCreator implements ArgumentConverter<Integer, MultiValueMap<?, ?>> {

    @Override
    public MultiValueMap<?, ?> convert(Integer value, ParameterMeta parameter) {
        return new LinkedMultiValueMap<>(value);
    }
}
