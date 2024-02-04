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
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.exception.DecodeException;
import org.apache.dubbo.remoting.http12.message.HttpMessageDecoder;
import org.apache.dubbo.remoting.http12.message.MediaType;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class MultipartDecoder implements HttpMessageDecoder {

    private final URL url;

    private final FrameworkModel frameworkModel;

    private final String headerContentType;

    private final CodecUtils codecUtils;

    private static final String CRLF = "\r\n";

    public MultipartDecoder(URL url, FrameworkModel frameworkModel, String contentType, CodecUtils codecUtils) {
        this.url = url;
        this.frameworkModel = frameworkModel;
        this.headerContentType = contentType;
        this.codecUtils = codecUtils;
    }

    @Override
    public Object decode(InputStream inputStream, Class<?> targetType, Charset charset) throws DecodeException {
        Object[] res = decode(inputStream, new Class[] {targetType}, charset);
        return res.length > 1 ? res : res[0];
    }

    @Override
    public Object[] decode(InputStream inputStream, Class<?>[] targetTypes, Charset charset) throws DecodeException {
        try {
            List<Part> parts = transferToParts(inputStream, headerContentType);
            if (parts.size() != targetTypes.length) {
                throw new DecodeException("The number of method parameters and multipart request bodies are different");
            }
            Object[] res = new Object[parts.size()];

            for (int i = 0; i < parts.size(); i++) {
                Part part = parts.get(i);

                if (Byte[].class.equals(targetTypes[i]) || byte[].class.equals(targetTypes[i])) {
                    res[i] = part.content;
                    continue;
                }
                res[i] = codecUtils
                        .determineHttpMessageDecoder(url, frameworkModel, part.headers.getContentType())
                        .decode(new ByteArrayInputStream(part.content), targetTypes[i], charset);
            }
            return res;
        } catch (IOException ioException) {
            throw new DecodeException("Decode multipart body failed:" + ioException.getMessage());
        }
    }

    private List<Part> transferToParts(InputStream inputStream, String contentType) throws IOException {
        String boundary = getBoundaryFromContentType(contentType);
        if (StringUtils.isEmpty(boundary)) {
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
                    byte[] endDelimiter = new byte[2];
                    if (inputStream.read(endDelimiter) != 2) {
                        throw new DecodeException("Boundary end is incomplete");
                    }
                    if (endDelimiter[0] == '-' && endDelimiter[1] == '-') {
                        return true;
                    } else {
                        inputStream.reset();
                        inputStream.skip(toWrite.length + 2);
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
