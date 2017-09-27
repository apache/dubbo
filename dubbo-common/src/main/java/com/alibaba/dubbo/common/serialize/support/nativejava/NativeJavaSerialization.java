/*
 * Copyright 1999-2012 Alibaba Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.alibaba.dubbo.common.serialize.support.nativejava;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.serialize.ObjectInput;
import com.alibaba.dubbo.common.serialize.ObjectOutput;
import com.alibaba.dubbo.common.serialize.Serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class NativeJavaSerialization implements Serialization {

    public static final String NAME = "nativejava";

    public byte getContentTypeId() {
        return 7;
    }

    public String getContentType() {
        return "x-application/nativejava";
    }

    public ObjectOutput serialize(URL url, OutputStream output) throws IOException {
        return new NativeJavaObjectOutput(output);
    }

    public ObjectInput deserialize(URL url, InputStream input) throws IOException {
        return new NativeJavaObjectInput(input);
    }
}
