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
package org.apache.dubbo.xds.test;

import org.apache.dubbo.remoting.RemotingServer;
import org.apache.dubbo.remoting.exchange.PortUnificationExchanger;

import java.io.IOException;
import java.io.Reader;
import java.lang.management.ManagementFactory;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import com.alibaba.fastjson2.JSON;

/** Optional, file-controlled transport fault for owned native priority fixtures only. */
final class NativeListenerProbe {
    private NativeListenerProbe() {}

    static void watch(Path runtime, int businessPort, int metadataPort) throws IOException {
        if (businessPort == metadataPort)
            throw new IllegalArgumentException("Listener fault needs a separate metadata port");
        RemotingServer business = listener(businessPort);
        RemotingServer metadata = listener(metadataPort);
        Path directory = runtime.resolve("provider-commands");
        Files.createDirectories(directory);
        Thread worker = new Thread(() -> run(directory, business, metadata, businessPort), "native-listener-probe");
        worker.setDaemon(true);
        worker.start();
    }

    private static RemotingServer listener(int port) {
        // The exchanger exposes this registry specifically for transport tests. Protocol wrapper
        // chains can inherit the empty getServers default, so do not use them to find listeners.
        for (RemotingServer remoting : PortUnificationExchanger.getServers().values()) {
            if (remoting != null
                    && !remoting.isClosed()
                    && remoting.getLocalAddress().getPort() == port) return remoting;
        }
        throw new IllegalStateException("Native listener is missing on port " + port);
    }

    private static void run(Path directory, RemotingServer business, RemotingServer metadata, int port) {
        while (!Thread.currentThread().isInterrupted()) {
            try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "*.request")) {
                for (Path request : files) {
                    String name = request.getFileName().toString().replaceFirst("\\.request$", "");
                    if (!name.matches("[A-Za-z0-9_-]{1,64}")) continue;
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put(
                            "process_start_ms",
                            ManagementFactory.getRuntimeMXBean().getStartTime());
                    try {
                        if (Files.size(request) > 65536) throw new IllegalArgumentException("Request is too large");
                        Properties properties = new Properties();
                        try (Reader reader = Files.newBufferedReader(request, StandardCharsets.UTF_8)) {
                            properties.load(reader);
                        }
                        if (!"close-business-listener".equals(properties.getProperty("method")))
                            throw new IllegalArgumentException("Unknown listener probe command");
                        business.close(0);
                        boolean listening = listening(port);
                        result.put("status", listening ? "ERROR" : "CLOSED");
                        result.put("business_listener_open", listening);
                        result.put("reported_closed", business.isClosed());
                        result.put("closed_port", port);
                        result.put("metadata_listener_open", !metadata.isClosed() && metadata.isBound());
                        result.put("completed_at_ms", System.currentTimeMillis());
                    } catch (Exception error) {
                        result.put("status", "ERROR");
                        result.put("error_type", error.getClass().getSimpleName());
                    }
                    Path temporary = directory.resolve(name + ".tmp");
                    Files.write(temporary, JSON.toJSONBytes(result));
                    Files.move(
                            temporary,
                            directory.resolve(name + ".json"),
                            StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE);
                    Files.delete(request);
                }
            } catch (IOException error) {
                // Test diagnostics contain exception types only, never application configuration.
                System.err.println(
                        "NATIVE_LISTENER_PROBE_ERROR " + error.getClass().getSimpleName());
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static boolean listening(int port) throws IOException {
        // NettyPortUnificationServer.close unbinds the listener without setting AbstractPeer's
        // closed flag. Verify an actual refused connection instead of inventing flag semantics.
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", port), 1000);
            return true;
        } catch (ConnectException refused) {
            return false;
        }
    }
}
