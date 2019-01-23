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

import com.alibaba.com.caucho.hessian.io.Deserializer;
import com.alibaba.com.caucho.hessian.io.HessianProtocolException;
import com.alibaba.com.caucho.hessian.io.SerializerFactory;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.StringUtils;

import java.util.Set;

public class Hessian2SerializerFactory extends SerializerFactory {
    private final static Logger logger = LoggerFactory.getLogger(Hessian2SerializerFactory.class);

    public static final SerializerFactory SERIALIZER_FACTORY = new Hessian2SerializerFactory();

    /**
     * For those classes are unknown in current classloader, record them in this set to avoid
     * frequently class loading and to reduce performance overhead.
     */
    private Set<String> typeNotFoundDeserializer = new ConcurrentHashSet<String>(8);

    private Hessian2SerializerFactory() {
    }

    @Override
    public ClassLoader getClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    @Override
    public Deserializer getDeserializer(String type)
            throws HessianProtocolException {
        if (StringUtils.isEmpty(type) || typeNotFoundDeserializer.contains(type)) {
            return null;
        }
        Deserializer deserializer = super.getDeserializer(type);
        if (deserializer == null) {
            typeNotFoundDeserializer.add(type);
            logger.warn("unable to find deserializer for string type:" + type);
        }
        return deserializer;
    }

}
