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
package org.apache.dubbo.remoting.http3;

import javax.crypto.NoSuchPaddingException;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSessionContext;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.concurrent.Executor;

import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.ssl.ApplicationProtocolNegotiator;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;

public class Netty4SslContext extends SslContext {

    public PrivateKey toPrivateKey0(InputStream in, String password)
            throws InvalidAlgorithmParameterException, NoSuchPaddingException, NoSuchAlgorithmException,
                    InvalidKeySpecException, IOException, KeyException {
        return toPrivateKey(in, password);
    }

    public X509Certificate[] toX509Certificates0(InputStream in) throws CertificateException {
        return toX509Certificates(in);
    }

    protected Netty4SslContext() {
        super();
    }

    @Override
    public long sessionCacheSize() {
        return super.sessionCacheSize();
    }

    @Override
    public long sessionTimeout() {
        return super.sessionTimeout();
    }

    @Override
    protected SslHandler newHandler(ByteBufAllocator alloc, boolean startTls) {
        return super.newHandler(alloc, startTls);
    }

    @Override
    public SslHandler newHandler(ByteBufAllocator alloc, Executor delegatedTaskExecutor) {
        return super.newHandler(alloc, delegatedTaskExecutor);
    }

    @Override
    protected SslHandler newHandler(ByteBufAllocator alloc, boolean startTls, Executor executor) {
        return super.newHandler(alloc, startTls, executor);
    }

    @Override
    protected SslHandler newHandler(ByteBufAllocator alloc, String peerHost, int peerPort, boolean startTls) {
        return super.newHandler(alloc, peerHost, peerPort, startTls);
    }

    @Override
    public SslHandler newHandler(
            ByteBufAllocator alloc, String peerHost, int peerPort, Executor delegatedTaskExecutor) {
        return super.newHandler(alloc, peerHost, peerPort, delegatedTaskExecutor);
    }

    @Override
    protected SslHandler newHandler(
            ByteBufAllocator alloc, String peerHost, int peerPort, boolean startTls, Executor delegatedTaskExecutor) {
        return super.newHandler(alloc, peerHost, peerPort, startTls, delegatedTaskExecutor);
    }

    @Override
    public boolean isClient() {
        return false;
    }

    @Override
    public List<String> cipherSuites() {
        return null;
    }

    @Override
    public ApplicationProtocolNegotiator applicationProtocolNegotiator() {
        return null;
    }

    @Override
    public SSLEngine newEngine(ByteBufAllocator byteBufAllocator) {
        return null;
    }

    @Override
    public SSLEngine newEngine(ByteBufAllocator byteBufAllocator, String s, int i) {
        return null;
    }

    @Override
    public SSLSessionContext sessionContext() {
        return null;
    }
}
