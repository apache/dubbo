/*
 * Copyright 2015 The gRPC Authors
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

package org.apache.dubbo.remoting.buffer;

/**
 * An interface for a byte buffer that can only be written to.
 * {@link WritableBuffer}s are a generic way to transfer bytes to
 * the concrete network transports, like Netty and OkHttp.
 */
public interface WritableBuffer {

  /**
   * Appends {@code length} bytes to the buffer from the source
   * array starting at {@code srcIndex}.
   *
   * @throws IndexOutOfBoundsException
   *         if the specified {@code srcIndex} is less than {@code 0},
   *         if {@code srcIndex + length} is greater than
   *            {@code src.length}, or
   *         if {@code length} is greater than {@link #writableBytes()}
   */
  void write(byte[] src, int srcIndex, int length);

  /**
   * Appends a single byte to the buffer.  This is slow so don't call it.
   */
  void write(byte b);


    /**
     * write int
     * @param value
     */
  void writeInt(int value);

  /**
   * Returns the number of bytes one can write to the buffer.
   */
  int writableBytes();

  /**
   * Returns the number of bytes one can read from the buffer.
   */
  int readableBytes();

  /**
   * Releases the buffer, indicating to the {@link WritableBufferAllocator} that
   * this buffer is no longer used and its resources can be reused.
   */
  void release();
}
