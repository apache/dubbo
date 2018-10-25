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
package org.apache.dubbo.common.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * Stream utils.
 */
public class StreamUtils {
    private StreamUtils() {
    }

    public static InputStream limitedInputStream(final InputStream is, final int limit) throws IOException {
        return new InputStream() {
            private int mPosition = 0, mMark = 0, mLimit = Math.min(limit, is.available());

            @Override
            public int read() throws IOException {
                if (mPosition < mLimit) {
                    mPosition++;
                    return is.read();
                }
                return -1;
            }

            @Override
            public int read(byte b[], int off, int len) throws IOException {
                if (b == null) {
                    throw new NullPointerException();
                }

                if (off < 0 || len < 0 || len > b.length - off) {
                    throw new IndexOutOfBoundsException();
                }

                if (mPosition >= mLimit) {
                    return -1;
                }

                if (mPosition + len > mLimit) {
                    len = mLimit - mPosition;
                }

                if (len <= 0) {
                    return 0;
                }

                is.read(b, off, len);
                mPosition += len;
                return len;
            }

            @Override
            public long skip(long len) throws IOException {
                if (mPosition + len > mLimit) {
                    len = mLimit - mPosition;
                }

                if (len <= 0) {
                    return 0;
                }

                is.skip(len);
                mPosition += len;
                return len;
            }

            @Override
            public int available() {
                return mLimit - mPosition;
            }

            @Override
            public boolean markSupported() {
                return is.markSupported();
            }

            @Override
            public void mark(int readlimit) {
                is.mark(readlimit);
                mMark = mPosition;
            }

            @Override
            public void reset() throws IOException {
                is.reset();
                mPosition = mMark;
            }

            @Override
            public void close() throws IOException {
                is.close();
            }
        };
    }

    public static InputStream markSupportedInputStream(final InputStream is, final int markBufferSize) {
        if (is.markSupported()) {
            return is;
        }

        return new InputStream() {
            byte[] mMarkBuffer;

            boolean mInMarked = false;
            boolean mInReset = false;
            boolean mDry = false;
            private int mPosition = 0;
            private int mCount = 0;

            @Override
            public int read() throws IOException {
                if (!mInMarked) {
                    return is.read();
                } else {
                    if (mPosition < mCount) {
                        byte b = mMarkBuffer[mPosition++];
                        return b & 0xFF;
                    }

                    if (!mInReset) {
                        if (mDry) {
                            return -1;
                        }

                        if (null == mMarkBuffer) {
                            mMarkBuffer = new byte[markBufferSize];
                        }
                        if (mPosition >= markBufferSize) {
                            throw new IOException("Mark buffer is full!");
                        }

                        int read = is.read();
                        if (-1 == read) {
                            mDry = true;
                            return -1;
                        }

                        mMarkBuffer[mPosition++] = (byte) read;
                        mCount++;

                        return read;
                    } else {
                        // mark buffer is used, exit mark status!
                        mInMarked = false;
                        mInReset = false;
                        mPosition = 0;
                        mCount = 0;

                        return is.read();
                    }
                }
            }

            /**
             * NOTE: the <code>readlimit</code> argument for this class
             *  has no meaning.
             */
            @Override
            public synchronized void mark(int readlimit) {
                mInMarked = true;
                mInReset = false;

                // mark buffer is not empty
                int count = mCount - mPosition;
                if (count > 0) {
                    System.arraycopy(mMarkBuffer, mPosition, mMarkBuffer, 0, count);
                    mCount = count;
                    mPosition = 0;
                }
            }

            @Override
            public synchronized void reset() throws IOException {
                if (!mInMarked) {
                    throw new IOException("should mark befor reset!");
                }

                mInReset = true;
                mPosition = 0;
            }

            @Override
            public boolean markSupported() {
                return true;
            }

            @Override
            public int available() throws IOException {
                int available = is.available();

                if (mInMarked && mInReset) {
                    available += mCount - mPosition;
                }

                return available;
            }

            @Override
            public void close() throws IOException {
                is.close();
            }
        };
    }

    public static InputStream markSupportedInputStream(final InputStream is) {
        return markSupportedInputStream(is, 1024);
    }

    public static void skipUnusedStream(InputStream is) throws IOException {
        if (is.available() > 0) {
            is.skip(is.available());
        }
    }
}
