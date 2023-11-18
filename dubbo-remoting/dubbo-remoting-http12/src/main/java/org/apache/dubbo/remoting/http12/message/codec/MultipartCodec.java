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
import org.apache.dubbo.remoting.http12.exception.DecodeException;
import org.apache.dubbo.remoting.http12.exception.EncodeException;
import org.apache.dubbo.remoting.http12.message.HttpMessageCodec;
import org.apache.dubbo.remoting.http12.message.HttpMessageCodecFactory;
import org.apache.dubbo.remoting.http12.message.MediaType;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.UploadContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

public class MultipartCodec implements HttpMessageCodec {

    private final URL url;

    private final FrameworkModel frameworkModel;

    private final String headerContentType;

    public MultipartCodec(URL url, FrameworkModel frameworkModel, String fullContentType) {
        this.url = url;
        this.frameworkModel = frameworkModel;
        this.headerContentType = fullContentType;
    }

    @Override
    public void encode(OutputStream outputStream, Object data) throws EncodeException {
        new JsonCodec().encode(outputStream, data);
    }

    @Override
    public Object decode(InputStream inputStream, Class<?> targetType) throws DecodeException {
        Object[] res = decode(inputStream, new Class[] {targetType});
        return res.length > 1 ? res : res[0];
    }

    @Override
    public Object[] decode(InputStream inputStream, Class<?>[] targetTypes) throws DecodeException {
        List<FileItem> items = getFileItems(inputStream, headerContentType);
        if (items.size() != targetTypes.length) {
            throw new DecodeException("The number of method parameters and multipart request bodies are different");
        }
        Object[] res = new Object[items.size()];
        try {
            List<HttpMessageCodecFactory> codecFactories = frameworkModel
                    .getExtensionLoader(HttpMessageCodecFactory.class)
                    .getActivateExtensions();

            for (int i = 0; i < items.size(); i++) {
                FileItem part = items.get(i);
                if (Byte[].class.equals(targetTypes[i]) || byte[].class.equals(targetTypes[i])) {
                    res[i] = part.get();
                    continue;
                }
                String contentType = part.getContentType();
                boolean decoded = false;
                for (HttpMessageCodecFactory factory : codecFactories) {
                    if (factory.support(contentType)) {
                        res[i] = factory.createCodec(url, frameworkModel, contentType)
                                .decode(part.getInputStream(), targetTypes[i]);
                        decoded = true;
                    }
                }
                if (!decoded) {
                    throw new DecodeException(
                            "No available codec found for content type:" + contentType + ",body part index:" + i);
                }
            }
        } catch (IOException ioException) {
            throw new DecodeException("Decode multipart body failed:" + ioException.getMessage());
        }
        return res;
    }

    private static List<FileItem> getFileItems(InputStream inputStream, String fullContentType) {
        List<FileItem> items;
        try {
            ServletFileUpload fileUpload = new ServletFileUpload(new DiskFileItemFactory());
            int contentLen = inputStream.available();
            items = fileUpload.parseRequest(new UploadContext() {
                @Override
                public long contentLength() {
                        return contentLen;
                }

                @Override
                public String getCharacterEncoding() {
                    return StandardCharsets.UTF_8.name();
                }

                @Override
                public String getContentType() {
                    return fullContentType;
                }

                @Override
                public int getContentLength() {
                    return (int) contentLength();
                }

                @Override
                public InputStream getInputStream() {
                    return inputStream;
                }
            });
        } catch (Exception exception) {
            throw new DecodeException(exception);
        }
        return items;
    }

    @Override
    public MediaType responseContentType() {
        return MediaType.APPLICATION_JSON_VALUE;
    }

    @Override
    public MediaType contentType() {
        return MediaType.MULTIPART_FORM_DATA;
    }
}
