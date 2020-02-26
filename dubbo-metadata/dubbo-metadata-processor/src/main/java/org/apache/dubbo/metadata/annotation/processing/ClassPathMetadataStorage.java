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
package org.apache.dubbo.metadata.annotation.processing;


import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.FileObject;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static javax.tools.StandardLocation.CLASS_OUTPUT;
import static org.apache.dubbo.metadata.annotation.processing.util.LoggerUtils.info;
import static org.apache.dubbo.metadata.annotation.processing.util.LoggerUtils.warn;

/**
 * A storage class for metadata under class path
 */
public class ClassPathMetadataStorage {

    private final Filer filer;

    public ClassPathMetadataStorage(ProcessingEnvironment processingEnv) {
        this.filer = processingEnv.getFiler();
    }

    public void write(Supplier<String> contentSupplier, String resourceName) {
        try (Writer writer = getWriter(resourceName)) {
            writer.write(contentSupplier.get());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> Optional<T> read(String resourceName, Function<Reader, T> consumer) {
        if (exists(resourceName)) {
            try (Reader reader = getReader(resourceName)) {
                return ofNullable(consumer.apply(reader));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return empty();
    }

    private boolean exists(String resourceName) {

        return getResource(resourceName)
                .map(FileObject::toUri)
                .map(File::new)
                .map(File::exists)
                .orElse(false);
    }

    private Reader getReader(String resourceName) {
        return getResource(resourceName).map(fileObject -> {
            try {
                return fileObject.openReader(false);
            } catch (IOException e) {
            }
            return null;
        }).orElse(null);
    }

    private FileObject createResource(String resourceName) throws IOException {
        return filer.createResource(CLASS_OUTPUT, "", resourceName);
    }

    private Optional<FileObject> getResource(String resourceName) {
        try {
            FileObject fileObject = filer.getResource(CLASS_OUTPUT, "", resourceName);
            return ofNullable(fileObject);
        } catch (IOException e) {
            warn(e.getMessage());
        }
        return empty();
    }

    private Writer getWriter(String resourceName) throws IOException {
        FileObject fileObject = createResource(resourceName);
        info("The resource[path : %s , deleted : %s] will be written", fileObject.toUri().getPath(), fileObject.delete());
        return fileObject.openWriter();
    }
}
