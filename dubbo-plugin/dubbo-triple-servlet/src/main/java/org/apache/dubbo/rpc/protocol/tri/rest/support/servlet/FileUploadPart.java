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
package org.apache.dubbo.rpc.protocol.tri.rest.support.servlet;

import org.apache.dubbo.common.io.StreamUtils;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.rpc.protocol.tri.rest.RestException;

import javax.servlet.http.Part;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

public final class FileUploadPart implements Part {

    private final HttpRequest.FileUpload fileUpload;

    public FileUploadPart(HttpRequest.FileUpload fileUpload) {
        this.fileUpload = fileUpload;
    }

    @Override
    public InputStream getInputStream() {
        return fileUpload.inputStream();
    }

    @Override
    public String getContentType() {
        return fileUpload.contentType();
    }

    @Override
    public String getName() {
        return fileUpload.name();
    }

    @Override
    public String getSubmittedFileName() {
        return fileUpload.filename();
    }

    @Override
    public long getSize() {
        return fileUpload.size();
    }

    @Override
    public void write(String fileName) {
        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            StreamUtils.copy(fileUpload.inputStream(), fos);
        } catch (IOException e) {
            throw new RestException(e);
        }
    }

    @Override
    public void delete() {}

    @Override
    public String getHeader(String name) {
        return null;
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return null;
    }

    @Override
    public Collection<String> getHeaderNames() {
        return Collections.emptyList();
    }
}
