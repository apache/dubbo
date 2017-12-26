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

package com.alibaba.com.caucho.hessian.io;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.EnumSet;

public class EnumSetSerializer extends AbstractSerializer {
    private static EnumSetSerializer SERIALIZER = new EnumSetSerializer();

    public static EnumSetSerializer getInstance() {
        return SERIALIZER;
    }

    @Override
    public void writeObject(Object obj, AbstractHessianOutput out) throws IOException {
        if (obj == null) {
            out.writeNull();
        } else {
            try {
                Field field = EnumSet.class.getDeclaredField("elementType");
                field.setAccessible(true);
                Class type = (Class) field.get(obj);
                EnumSet enumSet = (EnumSet) obj;
                Object[] objects = enumSet.toArray();
                out.writeObject(new EnumSetHandler(type, objects));
            } catch (Throwable t) {
                throw new IOException(t);
            }
        }
    }
}
