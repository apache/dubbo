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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/** Test-only opaque TCP relay: preserves pilot-agent's authenticated TLS connection to Istiod. */
public final class IstiodRelay {
    private static final Set<Pair> PAIRS = ConcurrentHashMap.newKeySet();
    private static final ExecutorService WORKERS = Executors.newCachedThreadPool();
    private static final AtomicLong FORWARDED = new AtomicLong();
    private static final AtomicLong DROPPED = new AtomicLong();
    private static final AtomicLong REJECTED = new AtomicLong();
    private static final AtomicLong CONNECT_ERRORS = new AtomicLong();
    private static final AtomicLong REQUEST_BYTES = new AtomicLong();
    private static final AtomicLong RESPONSE_BYTES = new AtomicLong();
    private static Path runtime = Paths.get("/relay");

    private static boolean blocked() {
        return Files.isRegularFile(runtime.resolve("block"));
    }

    public static void main(String[] args) throws Exception {
        if ((args.length != 2 && args.length != 3) || !args[0].matches("[A-Za-z0-9.-]+"))
            throw new IllegalArgumentException("Usage: IstiodRelay upstream-host upstream-port [runtime-dir]");
        if (args.length == 3) runtime = Paths.get(args[2]);
        int port = Integer.parseInt(args[1]);
        if (port < 1 || port > 65535) throw new IllegalArgumentException("Invalid upstream port");
        Files.createDirectories(runtime);
        try (ServerSocket listener = new ServerSocket(15013, 16, InetAddress.getByName("127.0.0.1"))) {
            Files.write(runtime.resolve("ready"), new byte[] {1});
            Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(() -> {
                if (blocked()) for (Pair pair : PAIRS) pair.close(true);
                try {
                    String status = "{\"blocked\":" + blocked() + ",\"active\":" + PAIRS.size()
                            + ",\"forwarded\":" + FORWARDED.get() + ",\"dropped_by_block\":" + DROPPED.get()
                            + ",\"rejected\":" + REJECTED.get() + ",\"upstream_connect_errors\":" + CONNECT_ERRORS.get()
                            + ",\"client_to_server_bytes\":" + REQUEST_BYTES.get() + ",\"server_to_client_bytes\":" + RESPONSE_BYTES.get()
                            + ",\"process_start_ms\":" + ManagementFactory.getRuntimeMXBean().getStartTime() + "}";
                    Path temporary = runtime.resolve("status.tmp");
                    Files.write(temporary, status.getBytes(StandardCharsets.UTF_8));
                    Files.move(temporary, runtime.resolve("status.json"), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ignored) {
                    // No protocol bytes, headers or credentials are inspected or logged.
                }
            }, 0, 100, TimeUnit.MILLISECONDS);
            while (true) {
                Socket downstream = listener.accept();
                if (blocked() || PAIRS.size() >= 8) {
                    REJECTED.incrementAndGet();
                    downstream.close();
                    continue;
                }
                WORKERS.execute(() -> {
                    Socket upstream = new Socket();
                    try {
                        upstream.connect(new InetSocketAddress(args[0], port), 5000);
                        Pair pair = new Pair(downstream, upstream);
                        PAIRS.add(pair);
                        if (blocked()) {
                            pair.close(true);
                            return;
                        }
                        FORWARDED.incrementAndGet();
                        WORKERS.execute(() -> pair.pipe(downstream, upstream, REQUEST_BYTES));
                        WORKERS.execute(() -> pair.pipe(upstream, downstream, RESPONSE_BYTES));
                    } catch (IOException error) {
                        CONNECT_ERRORS.incrementAndGet();
                        try { downstream.close(); } catch (IOException ignored) {}
                        try { upstream.close(); } catch (IOException ignored) {}
                    }
                });
            }
        }
    }

    private static final class Pair {
        private final Socket downstream;
        private final Socket upstream;
        private final AtomicBoolean closed = new AtomicBoolean();

        Pair(Socket downstream, Socket upstream) {
            this.downstream = downstream;
            this.upstream = upstream;
        }

        void pipe(Socket source, Socket destination, AtomicLong bytes) {
            try {
                InputStream input = source.getInputStream();
                OutputStream output = destination.getOutputStream();
                byte[] buffer = new byte[16384];
                int length;
                while ((length = input.read(buffer)) != -1) {
                    output.write(buffer, 0, length);
                    output.flush();
                    bytes.addAndGet(length);
                }
            } catch (IOException ignored) {
                // Real TCP closure is the intended fault signal.
            } finally {
                close(false);
            }
        }

        void close(boolean injected) {
            if (!closed.compareAndSet(false, true)) return;
            if (injected) DROPPED.incrementAndGet();
            try { downstream.close(); } catch (IOException ignored) {}
            try { upstream.close(); } catch (IOException ignored) {}
            PAIRS.remove(this);
        }
    }
}
