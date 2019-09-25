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
package org.apache.dubbo.metadata.annotation.processing.util;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * @since 2.7.5
 */
public interface TypeUtils {

    List<String> SIMPLE_TYPES = asList(
            Void.class.getName(),
            Boolean.class.getName(),
            Character.class.getName(),
            Byte.class.getName(),
            Short.class.getName(),
            Integer.class.getName(),
            Long.class.getName(),
            Float.class.getName(),
            Double.class.getName(),
            String.class.getName(),
            BigDecimal.class.getName(),
            BigInteger.class.getName(),
            Date.class.getName()
    );

    static boolean isSimpleType(Element element) {
        return isSimpleType(element.asType());
    }

    static boolean isSimpleType(TypeMirror type) {
        return SIMPLE_TYPES.contains(type.toString());
    }

    static TypeElement asType(Element element) {
        List<TypeElement> types = ElementFilter.typesIn(asList(element));
        return types.isEmpty() ? null : types.get(0);
    }
}
