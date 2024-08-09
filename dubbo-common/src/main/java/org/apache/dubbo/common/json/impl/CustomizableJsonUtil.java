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
package org.apache.dubbo.common.json.impl;

import org.apache.dubbo.common.extension.DisableInject;
import org.apache.dubbo.common.json.JsonUtilCustomizer;

public abstract class CustomizableJsonUtil<READER, WRITER> extends AbstractJsonUtilImpl {

    private JsonUtilCustomizer<Object> customizer;

    private volatile READER reader;
    private volatile WRITER writer;

    @DisableInject
    public final void setCustomizer(JsonUtilCustomizer<Object> customizer) {
        this.customizer = customizer;
    }

    public final boolean hasCustomizer() {
        return customizer != null;
    }

    public READER getReader() {
        READER reader = this.reader;
        if (reader == null) {
            synchronized (this) {
                reader = this.reader;
                if (reader == null) {
                    this.reader = reader = createReader();
                }
            }
        }
        return reader;
    }

    public WRITER getWriter() {
        WRITER writer = this.writer;
        if (writer == null) {
            synchronized (this) {
                writer = this.writer;
                if (writer == null) {
                    this.writer = writer = createWriter();
                }
            }
        }
        return writer;
    }

    protected READER createReader() {
        READER reader = newReader();
        if (customizer != null) {
            customizer.customize(reader);
        }
        return reader;
    }

    protected WRITER createWriter() {
        WRITER writer = newWriter();
        if (customizer != null) {
            customizer.customize(writer);
        }
        return writer;
    }

    protected READER newReader() {
        throw new UnsupportedOperationException();
    }

    protected WRITER newWriter() {
        throw new UnsupportedOperationException();
    }
}
