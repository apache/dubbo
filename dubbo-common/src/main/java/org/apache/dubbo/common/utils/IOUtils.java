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
package org.apache.dubbo.common.utils;

import org.apache.dubbo.common.constants.CommonConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Miscellaneous io utility methods.
 * Mainly for internal use within the framework.
 *
 * @author william.liangf
 * @since 2.0.7
 */
public class IOUtils {
    private static final int BUFFER_SIZE = 1024 * 8;
    public static final int EOF = -1;

    private IOUtils() {
    }

    /**
     * write.
     *
     * @param is InputStream instance.
     * @param os OutputStream instance.
     * @return count.
     * @throws IOException If an I/O error occurs
     */
    public static long write(InputStream is, OutputStream os) throws IOException {
        return write(is, os, BUFFER_SIZE);
    }

    /**
     * write.
     *
     * @param is         InputStream instance.
     * @param os         OutputStream instance.
     * @param bufferSize buffer size.
     * @return count.
     * @throws IOException If an I/O error occurs
     */
    public static long write(InputStream is, OutputStream os, int bufferSize) throws IOException {
        byte[] buff = new byte[bufferSize];
        return write(is, os, buff);
    }

    /**
     * write.
     *
     * @param input  InputStream instance.
     * @param output OutputStream instance.
     * @param buffer buffer byte array
     * @return count.
     * @throws IOException If an I/O error occurs
     */
    public static long write(final InputStream input, final OutputStream output, final byte[] buffer) throws IOException {
        long count = 0;
        int n;
        while (EOF != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    /**
     * read string.
     *
     * @param reader Reader instance.
     * @return String.
     * @throws IOException If an I/O error occurs
     */
    public static String read(Reader reader) throws IOException {
        try (StringWriter writer = new StringWriter()) {
            write(reader, writer);
            return writer.getBuffer().toString();
        }
    }

    /**
     * write string.
     *
     * @param writer Writer instance.
     * @param string String.
     * @throws IOException If an I/O error occurs
     */
    public static long write(Writer writer, String string) throws IOException {
        try (Reader reader = new StringReader(string)) {
            return write(reader, writer);
        }
    }

    /**
     * write.
     *
     * @param reader Reader.
     * @param writer Writer.
     * @return count.
     * @throws IOException If an I/O error occurs
     */
    public static long write(Reader reader, Writer writer) throws IOException {
        return write(reader, writer, BUFFER_SIZE);
    }

    /**
     * write.
     *
     * @param reader     Reader.
     * @param writer     Writer.
     * @param bufferSize buffer size.
     * @return count.
     * @throws IOException If an I/O error occurs
     */
    public static long write(Reader reader, Writer writer, int bufferSize) throws IOException {
        int read;
        long total = 0;
        char[] buf = new char[bufferSize];
        while ((read = reader.read(buf)) != -1) {
            writer.write(buf, 0, read);
            total += read;
        }
        return total;
    }

    /**
     * read lines.
     *
     * @param file file.
     * @return lines.
     * @throws IOException If an I/O error occurs
     */
    public static String[] readLines(File file) throws IOException {
        if (file == null || !file.exists() || !file.canRead()) {
            return new String[0];
        }

        return readLines(new FileInputStream(file));
    }

    /**
     * read lines.
     *
     * @param is input stream.
     * @return lines.
     * @throws IOException If an I/O error occurs
     */
    public static String[] readLines(InputStream is) throws IOException {
        List<String> lines = new ArrayList<String>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            return lines.toArray(new String[0]);
        }
    }

    /**
     * write lines.
     *
     * @param os    output stream.
     * @param lines lines.
     * @throws IOException If an I/O error occurs
     */
    public static void writeLines(OutputStream os, String[] lines) throws IOException {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(os))) {
            for (String line : lines) {
                writer.println(line);
            }
            writer.flush();
        }
    }

    /**
     * write lines.
     *
     * @param file  file.
     * @param lines lines.
     * @throws IOException If an I/O error occurs
     */
    public static void writeLines(File file, String[] lines) throws IOException {
        if (file == null) {
            throw new IOException("File is null.");
        }
        writeLines(new FileOutputStream(file), lines);
    }

    /**
     * append lines.
     *
     * @param file  file.
     * @param lines lines.
     * @throws IOException If an I/O error occurs
     */
    public static void appendLines(File file, String[] lines) throws IOException {
        if (file == null) {
            throw new IOException("File is null.");
        }
        writeLines(new FileOutputStream(file, true), lines);
    }


    /**
     * use like spring code
     * @param resourceLocation
     * @return
     */
    public static URL getURL(String resourceLocation) throws FileNotFoundException {
        Assert.notNull(resourceLocation, "Resource location must not be null");
        if (resourceLocation.startsWith(CommonConstants.CLASSPATH_URL_PREFIX)) {
            String path = resourceLocation.substring(CommonConstants.CLASSPATH_URL_PREFIX.length());
            ClassLoader cl = ClassUtils.getClassLoader();
            URL url = (cl != null ? cl.getResource(path) : ClassLoader.getSystemResource(path));
            if (url == null) {
                String description = "class path resource [" + path + "]";
                throw new FileNotFoundException(description +
                        " cannot be resolved to URL because it does not exist");
            }
            return url;
        }
        try {
            // try URL
            return new URL(resourceLocation);
        }
        catch (MalformedURLException ex) {
            // no URL -> treat as file path
            try {
                return new File(resourceLocation).toURI().toURL();
            }
            catch (MalformedURLException ex2) {
                throw new FileNotFoundException("Resource location [" + resourceLocation +
                        "] is neither a URL not a well-formed file path");
            }
        }
    }
}
