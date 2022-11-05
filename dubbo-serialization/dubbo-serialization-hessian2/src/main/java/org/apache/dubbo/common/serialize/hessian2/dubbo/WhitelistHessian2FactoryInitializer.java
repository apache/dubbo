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
package org.apache.dubbo.common.serialize.hessian2.dubbo;

import org.apache.dubbo.common.serialize.hessian2.Hessian2SerializerFactory;
import org.apache.dubbo.common.utils.StringUtils;

import com.alibaba.com.caucho.hessian.io.SerializerFactory;

/**
 * see https://github.com/ebourg/hessian/commit/cf851f5131707891e723f7f6a9718c2461aed826
 */
public class WhitelistHessian2FactoryInitializer extends AbstractHessian2FactoryInitializer {

    @Override
    public SerializerFactory createSerializerFactory() {
        SerializerFactory serializerFactory = new Hessian2SerializerFactory();
        if ("true".equals(WHITELIST)) {
            serializerFactory.getClassFactory().setWhitelist(true);
            if (StringUtils.isNotEmpty(ALLOW)) {
                for (String pattern : ALLOW.split(";")) {
                    serializerFactory.getClassFactory().allow(pattern);
                }
            }
        } else {
            serializerFactory.getClassFactory().setWhitelist(false);
            if (StringUtils.isNotEmpty(DENY)) {
                for (String pattern : DENY.split(";")) {
                    serializerFactory.getClassFactory().deny(pattern);
                }
            }
        }
        serializerFactory.setAllowNonSerializable(Boolean.parseBoolean(ALLOW_NON_SERIALIZABLE));
        serializerFactory.getClassFactory().allow("org.apache.dubbo.*");
        return serializerFactory;
    }

}
