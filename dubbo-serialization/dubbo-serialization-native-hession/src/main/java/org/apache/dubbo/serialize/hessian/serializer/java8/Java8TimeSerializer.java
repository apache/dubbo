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

package org.apache.dubbo.serialize.hessian.serializer.java8;


import com.caucho.hessian.io.AbstractHessianOutput;
import com.caucho.hessian.io.AbstractSerializer;

import java.io.IOException;
import java.lang.reflect.Constructor;

public class Java8TimeSerializer<T> extends AbstractSerializer {

    // Type of handle
    private Class<T> handleType;

    private Java8TimeSerializer(Class<T> handleType) {
        this.handleType = handleType;
    }

    public static <T> Java8TimeSerializer<T> create(Class<T> handleType) {
        return new Java8TimeSerializer<T>(handleType);
    }

    @Override
    public void writeObject(Object obj, AbstractHessianOutput out) throws IOException {
        if (obj == null) {
            out.writeNull();
            return;
        }

        T handle = null;
        try {
            Constructor<T> constructor = handleType.getConstructor(Object.class);
            handle = constructor.newInstance(obj);
        } catch (Exception e) {
            throw new RuntimeException("the class :" + handleType.getName() + " construct failed:" + e.getMessage(), e);
        }

        out.writeObject(handle);
    }
}
