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

package org.apache.dubbo.rpc.protocol.tri.rest.support.basic

import org.apache.dubbo.common.URL
import org.apache.dubbo.remoting.http12.HttpChannel
import org.apache.dubbo.remoting.http12.HttpMethods
import org.apache.dubbo.remoting.http12.HttpRequest
import org.apache.dubbo.remoting.http12.HttpUtils
import org.apache.dubbo.remoting.http12.RequestMetadata
import org.apache.dubbo.remoting.http12.h1.Http1InputMessage
import org.apache.dubbo.remoting.http12.h2.Http2InputMessageFrame
import org.apache.dubbo.remoting.http12.message.MediaType
import org.apache.dubbo.remoting.http12.rest.Mapping
import org.apache.dubbo.remoting.http12.rest.Param
import org.apache.dubbo.remoting.http12.rest.ParamType
import org.apache.dubbo.rpc.model.FrameworkModel
import org.apache.dubbo.rpc.protocol.tri.RpcInvocationBuildContext
import org.apache.dubbo.rpc.protocol.tri.TripleConstants
import org.apache.dubbo.rpc.protocol.tri.h12.AbstractServerTransportListener
import org.apache.dubbo.rpc.protocol.tri.h12.http1.DefaultHttp11ServerTransportListener
import org.apache.dubbo.rpc.protocol.tri.rest.service.DemoServiceImpl
import org.apache.dubbo.rpc.protocol.tri.rest.test.BaseServiceTest
import org.apache.dubbo.rpc.protocol.tri.test.MockH2StreamChannel
import org.apache.dubbo.rpc.protocol.tri.test.TestRequest
import org.apache.dubbo.rpc.protocol.tri.test.TestProtocol
import org.apache.dubbo.rpc.protocol.tri.test.TestRunnerBuilder
import org.apache.dubbo.rpc.protocol.tri.test.TestServerTransportListener

import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory
import spock.lang.Shared

import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

class HttpPostRequestDecoderLifecycleTest extends BaseServiceTest {

    @Shared
    UploadServiceImpl uploadService = new UploadServiceImpl()

    @Override
    void setupService(TestRunnerBuilder builder) {
        builder.provider(new DemoServiceImpl())
        builder.provider(UploadService, uploadService)
    }

    def "completed form request should release post data"() {
        given:
            def trackedRequests = trackedPostRequests()
            def request = new TestRequest(
                path: '/argTest',
                contentType: MediaType.APPLICATION_FROM_URLENCODED,
                body: 'name=Sam&age=8'
            )
        expect:
            runner.post(request) == 'Sam is 8 years old'
            trackedPostRequests() == trackedRequests
    }

    def "HTTP/1 multipart data should be released after exceptional completion"() {
        given:
            uploadService.reset()
            def trackedRequests = trackedPostRequests()
            def boundary = 'dubbo-test-boundary'
            def content = 'multipart content'
            def listener = new DirectHttp11ServerTransportListener(
                new MockH2StreamChannel(), testUrl(), FrameworkModel.defaultModel()
            )
            def request = new TestRequest(
                method: HttpMethods.POST.name(),
                path: '/upload',
                contentType: "${MediaType.MULTIPART_FORM_DATA.name}; boundary=${boundary}"
            )
        when:
            listener.onMetadata(request.toMetadata())
            listener.onData(new Http1InputMessage(new ByteArrayInputStream(multipartBody(boundary, content))))
        then:
            uploadService.fileUpload != null
            trackedPostRequests() == trackedRequests + 1
        when:
            uploadService.result.completeExceptionally(new RuntimeException('failed'))
        then:
            trackedPostRequests() == trackedRequests
        cleanup:
            uploadService.result?.complete('cleanup')
    }

    def "HTTP/2 multipart data should remain until application termination"() {
        given:
            uploadService.reset()
            def trackedRequests = trackedPostRequests()
            def boundary = 'dubbo-test-boundary'
            def content = 'multipart content'
            def listener = new TestServerTransportListener(
                new MockH2StreamChannel(), testUrl(), FrameworkModel.defaultModel()
            )
            def request = new TestRequest(
                method: HttpMethods.POST.name(),
                path: '/upload',
                contentType: "${MediaType.MULTIPART_FORM_DATA.name}; boundary=${boundary}"
            )
        when:
            listener.onMetadata(request.toMetadata())
            listener.onData(new Http2InputMessageFrame(
                new ByteArrayInputStream(multipartBody(boundary, content)), true
            ))
        then:
            uploadService.fileUpload != null
            trackedPostRequests() == trackedRequests + 1
        when:
            def inputStream = uploadService.fileUpload.inputStream()
            if (remoteCancellation) {
                listener.cancelByRemote(8)
            }
        then:
            new String(inputStream.bytes, StandardCharsets.UTF_8) == content
            trackedPostRequests() == trackedRequests + 1
        when:
            if (exceptionalCompletion) {
                uploadService.result.completeExceptionally(new RuntimeException('failed'))
            } else {
                uploadService.result.complete('ok')
            }
        then:
            trackedPostRequests() == trackedRequests
        cleanup:
            uploadService.result?.complete('cleanup')
        where:
            remoteCancellation | exceptionalCompletion
            true               | false
            true               | true
            false              | true
    }

    def "custom request adapter should not require decoder cleanup"() {
        given:
            def listener = new DefaultHttp11ServerTransportListener(
                Mock(HttpChannel), testUrl(), FrameworkModel.defaultModel()
            )
            def context = Stub(RpcInvocationBuildContext) {
                getAttributes() >> [(TripleConstants.HTTP_REQUEST_KEY): Stub(HttpRequest)]
            }
            def contextField = AbstractServerTransportListener.getDeclaredField('context')
            contextField.accessible = true
            contextField.set(listener, context)
            def observerField = DefaultHttp11ServerTransportListener.getDeclaredField('responseObserver')
            observerField.accessible = true
            def responseObserver = observerField.get(listener)
        when:
            responseObserver.onCompleted()
        then:
            noExceptionThrown()
    }

    private static URL testUrl() {
        return new URL(TestProtocol.NAME, TestProtocol.HOST, TestProtocol.PORT)
    }

    private static byte[] multipartBody(String boundary, String content) {
        return ("--${boundary}\r\n" +
            'Content-Disposition: form-data; name="file"; filename="test.txt"\r\n' +
            'Content-Type: text/plain\r\n\r\n' +
            content + "\r\n--${boundary}--\r\n").getBytes(StandardCharsets.UTF_8)
    }

    private static int trackedPostRequests() {
        def field = DefaultHttpDataFactory.getDeclaredField('requestFileDeleteMap')
        field.accessible = true
        return ((Map<?, ?>)field.get(HttpUtils.DATA_FACTORY)).size()
    }

    @Mapping('/')
    private interface UploadService {

        @Mapping('/upload')
        CompletableFuture<String> upload(
            @Param(value = 'file', type = ParamType.Part) HttpRequest.FileUpload fileUpload
        )
    }

    private static final class UploadServiceImpl implements UploadService {

        volatile HttpRequest.FileUpload fileUpload
        CompletableFuture<String> result

        void reset() {
            fileUpload = null
            result = new CompletableFuture<>()
        }

        @Override
        CompletableFuture<String> upload(HttpRequest.FileUpload fileUpload) {
            this.fileUpload = fileUpload
            return result
        }
    }

    private static final class DirectHttp11ServerTransportListener extends DefaultHttp11ServerTransportListener {

        DirectHttp11ServerTransportListener(HttpChannel httpChannel, URL url, FrameworkModel frameworkModel) {
            super(httpChannel, url, frameworkModel)
        }

        @Override
        protected Executor initializeExecutor(URL url, RequestMetadata metadata) {
            return { Runnable command -> command.run() } as Executor
        }
    }
}
