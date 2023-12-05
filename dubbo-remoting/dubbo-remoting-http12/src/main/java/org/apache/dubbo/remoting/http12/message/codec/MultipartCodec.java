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
package org.apache.dubbo.remoting.http12.message.codec;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.exception.DecodeException;
import org.apache.dubbo.remoting.http12.exception.EncodeException;
import org.apache.dubbo.remoting.http12.message.HttpMessageCodec;
import org.apache.dubbo.remoting.http12.message.HttpMessageCodecFactory;
import org.apache.dubbo.remoting.http12.message.MediaType;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class MultipartCodec implements HttpMessageCodec {

    private final URL url;

    private final FrameworkModel frameworkModel;

    private final String headerContentType;

    private static final String CRLF = "\r\n";

    public MultipartCodec(URL url, FrameworkModel frameworkModel, String contentType) {
        this.url = url;
        this.frameworkModel = frameworkModel;
        this.headerContentType = contentType;
    }

    @Override
    public void encode(OutputStream outputStream, Object data) throws EncodeException {
        throw new EncodeException("MultipartCodec does not support encode .");
    }

    @Override
    public Object decode(InputStream inputStream, Class<?> targetType) throws DecodeException {
        Object[] res = decode(inputStream, new Class[] {targetType});
        return res.length > 1 ? res : res[0];
    }

    @Override
    public Object[] decode(InputStream inputStream, Class<?>[] targetTypes) throws DecodeException {
        try {
            List<Part> parts = transferToParts(inputStream, headerContentType);
            if (parts.size() != targetTypes.length) {
                throw new DecodeException("The number of method parameters and multipart request bodies are different");
            }
            Object[] res = new Object[parts.size()];

            List<HttpMessageCodecFactory> codecFactories = frameworkModel
                    .getExtensionLoader(HttpMessageCodecFactory.class)
                    .getActivateExtensions();

            for (int i = 0; i < parts.size(); i++) {
                Part part = parts.get(i);

                if (Byte[].class.equals(targetTypes[i]) || byte[].class.equals(targetTypes[i])) {
                    res[i] = part.content;
                    continue;
                }
                boolean decoded = false;

                for (HttpMessageCodecFactory factory : codecFactories) {
                    String contentType = part.headers.getContentType();
                    if (factory.codecSupport().supportDecode(contentType)) {
                        res[i] = factory.createCodec(url, frameworkModel, contentType)
                                .decode(new ByteArrayInputStream(part.content), targetTypes[i]);
                        decoded = true;
                    }
                }

                if (!decoded) {
                    throw new DecodeException("No available codec found for content type:"
                            + part.headers.getContentType() + ",body part index:" + i);
                }
            }
            return res;
        } catch (IOException ioException) {
            throw new DecodeException("Decode multipart body failed:" + ioException.getMessage());
        }
    }

    private List<Part> transferToParts(InputStream inputStream, String contentType) throws IOException {
        String boundary = getBoundaryFromContentType(contentType);
        if (boundary.isEmpty()) {
            throw new DecodeException("Invalid boundary in Content-Type: " + contentType);
        }

        final String delimiter = "--" + boundary;

        List<Part> parts = new ArrayList<>();
        boolean endOfStream = false;

        while (!endOfStream) {
            ByteArrayOutputStream partData = new ByteArrayOutputStream();
            HttpHeaders headers = new HttpHeaders();

            endOfStream = readPart(inputStream, delimiter, headers, partData);

            if (partData.size() > 0) {
                parts.add(new Part(partData.toByteArray(), headers));
            }
        }

        return parts;
    }

    private String getBoundaryFromContentType(String contentType) {
        String[] parts = contentType.split(";");
        for (String part : parts) {
            part = part.trim();
            if (part.startsWith("boundary=")) {
                return part.substring("boundary=".length()).trim();
            }
        }
        return null;
    }

    private boolean readPart(
            InputStream inputStream, String delimiter, HttpHeaders headers, ByteArrayOutputStream partData)
            throws IOException {
        // read and parse headers
        if (readHeaders(inputStream, headers, delimiter)) {
            // end of stream
            return true;
        }
        return readBody(inputStream, delimiter, partData);
    }

    private boolean readHeaders(InputStream inputStream, HttpHeaders httpHeaders, String delimiter) throws IOException {

        StringBuilder fullHeaderBuilder = new StringBuilder();
        String fullHeader = null;
        byte[] buffer = new byte[128];
        int len;
        boolean headerEnd = false;
        boolean streamEnd = true;
        final String endOfHeaderSign = CRLF + CRLF;

        byte[] delimiterBuffer = new byte[delimiter.length()];
        if (inputStream.read(delimiterBuffer) == -1) {
            return true;
        }
        String readDelimiter = new String(delimiterBuffer, StandardCharsets.US_ASCII);
        if (!Objects.equals(readDelimiter, delimiter)) {
            throw new DecodeException("Multipart body boundary are different from header");
        }
        while (!headerEnd) {

            inputStream.mark(Integer.MAX_VALUE);

            len = inputStream.read(buffer);
            if (len == -1) {
                break;
            }

            // read to 2*CRLF (end of header)
            String currentString = new String(buffer, 0, len, StandardCharsets.UTF_8);
            fullHeaderBuilder.append(currentString);

            // check if currentString contains CRLF
            int endIndex;
            if ((endIndex = fullHeaderBuilder.indexOf(endOfHeaderSign)) != -1) {
                // make stream reset to body start of current part
                inputStream.reset();
                if (inputStream.skip(endIndex + endOfHeaderSign.length()) == endIndex + endOfHeaderSign.length()) {
                    streamEnd = false;
                }
                headerEnd = true;
                fullHeader = fullHeaderBuilder.substring(delimiter.length(), endIndex);
            }
        }
        if (streamEnd && !headerEnd) {
            throw new DecodeException("Broken request: cannot found multipart body header end");
        }

        parseHeaderLine(httpHeaders, fullHeader.split(CRLF));

        if (httpHeaders.getContentType() == null) {
            httpHeaders.put(HttpHeaderNames.CONTENT_TYPE.getName(), Collections.singletonList("text/plain"));
        }

        return streamEnd;
    }

    private void parseHeaderLine(HttpHeaders headers, String[] headerLines) {
        for (String headerLine : headerLines) {
            int colonIndex = headerLine.indexOf(':');
            if (colonIndex != -1) {
                String name = headerLine.substring(0, colonIndex).trim();
                String value = headerLine.substring(colonIndex + 1).trim();
                headers.put(name, Collections.singletonList(value));
            }
        }
    }

    private boolean readBody(InputStream inputStream, String delimiter, ByteArrayOutputStream partData)
            throws IOException {
        byte[] buffer = new byte[256];
        int len;

        while (true) {
            inputStream.mark(Integer.MAX_VALUE);
            len = inputStream.read(buffer);
            if (len == -1) {
                return true;
            }
            String currentString = new String(buffer, 0, len, StandardCharsets.US_ASCII);
            if (currentString.contains(delimiter)) {
                int indexOfDelimiter = currentString.indexOf(delimiter);

                // skip the CRLF of data tail
                byte[] toWrite = new byte[indexOfDelimiter - 2];
                System.arraycopy(buffer, 0, toWrite, 0, indexOfDelimiter - 2);
                partData.write(toWrite);

                // check end delimiter (--\r\n) to determine if this part is the last body part
                // for compatibility with non-standard clients, we won't check the last CRLF
                if (currentString.length() > indexOfDelimiter + delimiter.length() + 1
                        && currentString.charAt(indexOfDelimiter + delimiter.length()) == '-'
                        && currentString.charAt(indexOfDelimiter + delimiter.length() + 1) == '-') {
                    return true;
                }

                // read from stream to check end delimiter
                else if (currentString.length() <= indexOfDelimiter + delimiter.length() + 1) {
                    inputStream.mark(2);
                    byte[] endDelimiter = new byte[2];
                    if (inputStream.read(endDelimiter) != 2) {
                        throw new DecodeException("Boundary end is incomplete");
                    }
                    if (endDelimiter[0] == '-' && endDelimiter[1] == '-') {
                        return true;
                    } else {
                        inputStream.reset();
                    }
                } else {
                    inputStream.reset();
                    inputStream.skip(toWrite.length + 2);
                }
                return false;
            }
            partData.write(buffer, 0, len);
        }
    }

    @Override
    public MediaType mediaType() {
        return MediaType.MULTIPART_FORM_DATA;
    }

    private static class Part {

        private final byte[] content;

        private final HttpHeaders headers;

        public Part(byte[] content, HttpHeaders headers) {
            this.content = content;
            this.headers = headers;
        }
    }
}
