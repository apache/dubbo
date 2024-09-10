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
package org.apache.dubbo.remoting.http12.message;

import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.netty4.NettyHttpHeaders;

import java.util.Map.Entry;

import io.netty.handler.codec.CharSequenceValueConverter;
import io.netty.handler.codec.DefaultHeaders;
import io.netty.handler.codec.Headers;
import io.netty.util.AsciiString;

public final class DefaultHttpHeaders extends NettyHttpHeaders<Headers<CharSequence, CharSequence, ?>> {

    public DefaultHttpHeaders() {
        super(new HeadersMap());
    }

    public DefaultHttpHeaders(Headers<CharSequence, CharSequence, ?> headers) {
        super(new HeadersMap(headers));
    }

    public DefaultHttpHeaders(HttpHeaders headers) {
        super(new HeadersMap(headers));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final class HeadersMap extends DefaultHeaders<CharSequence, CharSequence, HeadersMap> {

        HeadersMap() {
            this(16);
        }

        HeadersMap(Headers<?, ?, ?> headers) {
            this(headers.size());
            addImpl((Headers) headers);
        }

        HeadersMap(HttpHeaders headers) {
            this(headers.size());
            for (Entry<CharSequence, String> entry : headers) {
                add(entry.getKey(), entry.getValue());
            }
        }

        HeadersMap(int size) {
            super(
                    AsciiString.CASE_INSENSITIVE_HASHER,
                    CharSequenceValueConverter.INSTANCE,
                    NameValidator.NOT_NULL,
                    size,
                    (ValueValidator) ValueValidator.NO_VALIDATION);
        }

        @Override
        protected void validateName(NameValidator<CharSequence> validator, boolean forAdd, CharSequence name) {}

        @Override
        protected void validateValue(ValueValidator<CharSequence> validator, CharSequence name, CharSequence value) {}
    }
}
