/*
 * Copyright 2019 The gRPC Authors
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

import javax.annotation.concurrent.ThreadSafe;

import java.util.concurrent.ThreadLocalRandom;

@ThreadSafe // Except for impls/mocks in tests
interface ThreadSafeRandom {
  int nextInt(int bound);

  long nextLong();

  long nextLong(long bound);

  final class ThreadSafeRandomImpl implements ThreadSafeRandom {

    static final ThreadSafeRandom instance = new ThreadSafeRandomImpl();

    private ThreadSafeRandomImpl() {}

    @Override
    public int nextInt(int bound) {
      return ThreadLocalRandom.current().nextInt(bound);
    }

    @Override
    public long nextLong() {
      return ThreadLocalRandom.current().nextLong();
    }

    @Override
    public long nextLong(long bound) {
      return ThreadLocalRandom.current().nextLong(bound);
    }
  }
}
