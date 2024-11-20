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
package org.apache.dubbo.remoting.websocket;

import java.io.ByteArrayInputStream;

public class FinalFragmentByteArrayInputStream extends ByteArrayInputStream implements FinalFragment {

    private final boolean finalFragment;

    public FinalFragmentByteArrayInputStream(byte[] buf) {
        this(buf, 0, buf.length);
    }

    public FinalFragmentByteArrayInputStream(byte[] buf, boolean finalFragment) {
        this(buf, 0, buf.length, finalFragment);
    }

    public FinalFragmentByteArrayInputStream(byte[] buf, int offset, int length) {
        this(buf, offset, length, false);
    }

    public FinalFragmentByteArrayInputStream(byte[] buf, int offset, int length, boolean finalFragment) {
        super(buf, offset, length);
        this.finalFragment = finalFragment;
    }

    @Override
    public boolean isFinalFragment() {
        return finalFragment;
    }
}
