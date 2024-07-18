/*
 * Copyright 2020 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.xds.resource.grpc;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * A reference count wrapper for objects. This class does not take the ownership for the object,
 * but only provides usage counting. The real owner of the wrapped object is responsible for
 * managing the lifecycle of the object.
 *
 * <p>Intended for a container class to keep track of lifecycle for elements it contains. This
 * wrapper itself should never be returned to the consumers of the elements to avoid reference
 * counts being leaked.
 */
// TODO(chengyuanzhang): move this class into LoadStatsManager2.
final class ReferenceCounted<T> {
  private final T instance;
  private int refs;

  private ReferenceCounted(T instance) {
    this.instance = instance;
  }

  static <T> ReferenceCounted<T> wrap(T instance) {
    checkNotNull(instance, "instance");
    return new ReferenceCounted<>(instance);
  }

  void retain() {
    refs++;
  }

  void release() {
    checkState(refs > 0, "reference reached 0");
    refs--;
  }

  int getReferenceCount() {
    return refs;
  }

  T get() {
    return instance;
  }
}
