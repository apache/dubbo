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
package com.serialization;

import java.io.Serializable;
import java.util.List;

/**
 * Models loaded without OptionalDependency to exercise incomplete application classpaths.
 * @date 2026-11-05
 */
public class MissingTypeFixtures {
    public static class OptionalDependency {}

    public static class Healthy implements Serializable {
        public Nested value;
    }

    public static class Nested implements Serializable {}

    public static class Unrelated implements Serializable {}

    public static class MissingField {
        public OptionalDependency optional;
    }

    public static class MissingTransientField {
        public transient OptionalDependency optional;
    }

    public static class MissingGenericField {
        public List<OptionalDependency> optional;
        public Healthy healthy;
    }

    public interface Contract<T> {}

    public static class MissingGenericInterface implements Contract<OptionalDependency> {
        public Healthy healthy;
    }

    public static class Base<T> {}

    public static class MissingGenericSuperclass extends Base<OptionalDependency> {
        public Healthy healthy;
    }

    public static class MissingBound<T extends OptionalDependency> {
        public List<T> optional;
        public Healthy healthy;
    }

    public interface FieldService {
        Healthy read(MissingField value);
    }

    public interface TransientFieldService {
        Healthy read(MissingTransientField value);
    }

    public interface GenericParameterService {
        Healthy read(List<OptionalDependency> value);
    }

    public interface GenericFieldService {
        void read(MissingGenericField value);
    }

    public interface GenericInterfaceService {
        void read(MissingGenericInterface value);
    }

    public interface GenericSuperclassService {
        void read(MissingGenericSuperclass value);
    }

    public interface BoundService {
        void read(MissingBound<?> value);
    }

    public interface GenericMethodService {
        List<OptionalDependency> optional();

        Healthy read();
    }
}
