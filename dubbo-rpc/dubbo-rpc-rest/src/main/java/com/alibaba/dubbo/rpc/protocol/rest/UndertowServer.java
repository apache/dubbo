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

//import com.alibaba.dubbo.common.URL;
//import com.alibaba.dubbo.common.utils.StringUtils;
//import io.undertow.Undertow;
//import io.undertow.servlet.api.DeploymentInfo;
//import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
//import org.jboss.resteasy.spi.ResteasyDeployment;

/**
 * TODO this impl hasn't been well tested, and we can consider move undertow to a general remoting-http impl in the future
 *
 * @author lishen
 */
public class UndertowServer /*implements RestServer*/ {

//    // Note that UndertowJaxrsServer doesn't implement EmbeddedJaxrsServer
//
//    private final ResteasyDeployment deployment = new ResteasyDeployment();
//
//    private final UndertowJaxrsServer server = new UndertowJaxrsServer();
//
//    public void start(URL url) {
//        deployment.start();
//        DeploymentInfo deploymentInfo = server.undertowDeployment(deployment);
//        deploymentInfo.setContextPath("/");
//        deploymentInfo.setDeploymentName("dubbo-rest");
//        deploymentInfo.setClassLoader(Thread.currentThread().getContextClassLoader());
//        server.deploy(deploymentInfo);
//        server.start(Undertow.builder().addHttpListener(url.getPort(), url.getHost()));
//    }
//
//    public void deploy(Class resourceDef, Object resourceInstance, String contextPath) {
//        if (StringUtils.isEmpty(contextPath)) {
//            deployment.getRegistry().addResourceFactory(new DubboResourceFactory(resourceInstance, resourceDef));
//        } else {
//            deployment.getRegistry().addResourceFactory(new DubboResourceFactory(resourceInstance, resourceDef), contextPath);
//        }
//    }
//
//    public void undeploy(Class resourceDef) {
//        deployment.getRegistry().removeRegistrations(resourceDef);
//    }
//
//    public void deploy(Class resourceDef, Object resourceInstance) {
//        deploy(resourceDef, resourceInstance, "/");
//    }
//
//    public void stop() {
//        deployment.stop();
//        server.stop();
//    }
}
