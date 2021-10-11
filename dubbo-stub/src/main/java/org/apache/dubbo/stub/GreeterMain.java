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

package org.apache.dubbo.stub;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.api.Connection;
import org.apache.dubbo.remoting.api.SingleProtocolConnectionManager;

import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class GreeterMain {
    public static void main(String[] args) throws RemotingException, InterruptedException {
        final SingleProtocolConnectionManager connectionManager = new SingleProtocolConnectionManager();
        Connection connection = connectionManager.connect(URL.valueOf("tri://localhost:50053"));
        final GreeterStub stub = GreeterStub.newStub(connection);
        final HelloRequest request = HelloRequest.newBuilder()
                .setName("stub request")
                .build();
        final CountDownLatch latch = new CountDownLatch(1);
        stub.sayHello(request, new StreamObserver<HelloReply>() {
            @Override
            public void onNext(HelloReply data) {
                System.out.println("next");
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
            }

            @Override
            public void onCompleted() {
                System.out.println("Done");
            }
        });

        latch.await(30, TimeUnit.SECONDS);
    }
}
