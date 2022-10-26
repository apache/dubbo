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

package org.apache.dubbo.remoting.buffer;

/**
 * An allocator of buffers provided by the transport implementation to {@link MessageFramer} so
 * it can send chunks of data to the transport in a form that the transport can directly serialize.
 */
public interface WritableBufferAllocator {

  /**
   * Request a new {@link WritableBuffer} with the given {@code capacityHint}. The allocator is
   * free to return a buffer with a greater or lesser capacity.
   */
  WritableBuffer allocate(int capacityHint);
}
