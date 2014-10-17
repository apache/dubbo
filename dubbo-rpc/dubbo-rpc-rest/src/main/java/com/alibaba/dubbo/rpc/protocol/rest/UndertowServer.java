/**
 * Copyright 1999-2014 dangdang.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.protocol.rest;

/**
 * TODO implement this after migrating to servlet 3.x api
 *
 * @author lishen
 */
public class UndertowServer /*implements RestServer*/ {

//    // NOTEUndertowJaxrsServer doesn't implement EmbeddedJaxrsServer
//    private final UndertowJaxrsServer server = new UndertowJaxrsServer();
//
//    private final ResteasyDeployment deployment = new ResteasyDeployment();
//
//    public void start(String host, int port) {
//        deployment.start();
//        DeploymentInfo deploymentInfo = server.undertowDeployment(deployment);
//        deploymentInfo.setContextPath("/");
//        deploymentInfo.setDeploymentName("dubbo-rest");
//        deploymentInfo.setClassLoader(Thread.currentThread().getContextClassLoader());
//        server.start(Undertow.builder().addListener(port, host));
//        server.deploy(deploymentInfo);
////        server.start();
//    }
//
//    public void deploy(Class resourceDef, Object resourceInstance) {
//        deployment.getRegistry().addResourceFactory(new DubboResourceFactory(resourceInstance, resourceDef));
//    }
//
//    public void undeploy(Class resourceDef) {
//
//    }
//
//    public void stop() {
//
//    }

}
