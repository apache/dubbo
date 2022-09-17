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

package org.apache.dubbo.errorcode.linktest;

import org.apache.dubbo.errorcode.util.ErrorUrlUtils;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

/**
 * Link testing (fork-join) task.
 */
public class LinkTestingForkJoinTask extends RecursiveTask<Map<String, Boolean>> {

    private static final CloseableHttpClient HTTP_CLIENT = HttpClients.createDefault();

    private static final int THRESHOLD = 10;

    private final int start;

    private final int end;

    private final List<String> url;

    private static final ForkJoinPool FORK_JOIN_POOL = new ForkJoinPool();

    public LinkTestingForkJoinTask(int start, int end, List<String> url) {
        this.start = start;
        this.end = end;
        this.url = url;
    }

    @Override
    protected Map<String, Boolean> compute() {

        if (end - start >= THRESHOLD) {

            int middle = (start + end) / 2;

            LinkTestingForkJoinTask left = new LinkTestingForkJoinTask(start, middle, url);
            LinkTestingForkJoinTask right = new LinkTestingForkJoinTask(middle, end, url);

            left.fork();
            right.fork();

            Map<String, Boolean> leftR = left.join();
            Map<String, Boolean> rightR = right.join();

            Map<String, Boolean> result = new HashMap<>(end - start);

            result.putAll(leftR);
            result.putAll(rightR);

            return result;

        } else {

            HashMap<String, Boolean> result = new HashMap<>();

            for (int i = start; i < end; i++) {

                HttpGet getRequest = new HttpGet(url.get(i));
                getRequest.addHeader("Accept-Language", "zh-CN");

                try {

                    try (CloseableHttpResponse resp = HTTP_CLIENT.execute(getRequest)) {
                        result.put(url.get(i), resp.getStatusLine().getStatusCode() == 200);
                    }

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            return result;
        }
    }

    public static void closeHttpClient() {
        try {
            HTTP_CLIENT.close();
            FORK_JOIN_POOL.shutdown();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<String> findDocumentMissingErrorCodes(List<String> codes) {

        List<String> urls = codes.stream().distinct().sorted().map(ErrorUrlUtils::getErrorUrl).collect(Collectors.toList());
        LinkTestingForkJoinTask firstTask = new LinkTestingForkJoinTask(0, urls.size(), urls);

        Set<Map.Entry<String, Boolean>> linkResults = FORK_JOIN_POOL.invoke(firstTask).entrySet();

        return linkResults.stream()
            .filter(e -> !e.getValue())
            .map(Map.Entry::getKey)
            .map(ErrorUrlUtils::getErrorCodeThroughErrorUrl)
            .collect(Collectors.toList());
    }
}
