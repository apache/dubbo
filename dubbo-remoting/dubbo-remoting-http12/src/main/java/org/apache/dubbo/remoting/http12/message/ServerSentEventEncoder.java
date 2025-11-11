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

import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.http12.exception.EncodeException;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Encode the data according to the Server-Sent Events specification.
 * <p>
 * The formatted string follows the text/event-stream format as defined in the HTML specification.
 * Each field is formatted as a line with the field name, followed by a colon, followed by the field value,
 * and ending with a newline character.
 *
 * @see <a href="https://html.spec.whatwg.org/multipage/server-sent-events.html#event-stream-interpretation">Event stream interpretation</a>
 */
public final class ServerSentEventEncoder implements HttpMessageEncoder {

    private final HttpMessageEncoder httpMessageEncoder;

    public ServerSentEventEncoder(HttpMessageEncoder httpMessageEncoder) {
        this.httpMessageEncoder = httpMessageEncoder;
    }

    @Override
    public void encode(OutputStream outputStream, Object data, Charset charset) throws EncodeException {
        StringBuilder sb = new StringBuilder(256);

        if (data instanceof ServerSentEvent) {
            ServerSentEvent<?> event = (ServerSentEvent<?>) data;
            if (event.getId() != null) {
                appendField(sb, "id", event.getId());
            }
            if (event.getEvent() != null) {
                appendField(sb, "event", event.getEvent());
            }
            if (event.getRetry() != null) {
                appendField(sb, "retry", event.getRetry().toMillis());
            }
            if (event.getComment() != null) {
                sb.append(':')
                        .append(StringUtils.replace(event.getComment(), "\n", "\n:"))
                        .append('\n');
            }
            if (event.getData() != null) {
                encodeData(sb, event.getData(), charset);
            }
        } else {
            encodeData(sb, data, charset);
        }

        sb.append('\n');

        try {
            outputStream.write(sb.toString().getBytes(charset));
        } catch (Exception e) {
            throw new EncodeException("Error encoding ServerSentEvent", e);
        }
    }

    private void encodeData(StringBuilder sb, Object data, Charset charset) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(256);
        httpMessageEncoder.encode(bos, data, charset);
        String dataStr = new String(bos.toByteArray(), charset);
        List<String> lines = StringUtils.splitToList(dataStr, '\n');
        for (int i = 0, size = lines.size(); i < size; i++) {
            appendField(sb, "data", lines.get(i));
        }
    }

    private static void appendField(StringBuilder sb, String name, Object value) {
        sb.append(name).append(':').append(value).append('\n');
    }

    @Override
    public String contentType() {
        // An idea:sse use text/event-stream regardless of the underlying data encoder...
        return MediaType.TEXT_EVENT_STREAM.getName();
    }

    @Override
    public MediaType mediaType() {
        return MediaType.TEXT_EVENT_STREAM;
    }

    @Override
    public boolean supports(String mediaType) {
        return httpMessageEncoder.supports(mediaType);
    }
}
