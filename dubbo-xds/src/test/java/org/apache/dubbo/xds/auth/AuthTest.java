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
package org.apache.dubbo.xds.auth;

import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.xds.istio.IstioConstant;
import org.apache.dubbo.xds.kubernetes.KubeApiClient;
import org.apache.dubbo.xds.kubernetes.KubeEnv;
import org.apache.dubbo.xds.security.authz.rule.source.KubeRuleProvider;
import org.apache.dubbo.xds.security.authz.rule.source.MapRuleFactory;

public class AuthTest {

//    @Test
    public void authZTest() throws Exception {


        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        System.setProperty("NAMESPACE", "foo");
        System.setProperty("SERVICE_NAME", "httpbin");
        System.setProperty("API_SERVER_PATH", "https://127.0.0.1:6443");
        System.setProperty("SA_CA_PATH", "/Users/nameles/Desktop/test_secrets/kubernetes.io/serviceaccount/ca.crt");
        System.setProperty(
                "SA_TOKEN_PATH", "/Users/nameles/Desktop/test_secrets/kubernetes.io/serviceaccount/token_foo");

        KubeEnv kubeEnv = new KubeEnv(applicationModel);
        kubeEnv.setNamespace("foo");
        kubeEnv.setEnableSsl(true);
        kubeEnv.setApiServerPath("https://127.0.0.1:6443");
        kubeEnv.setServiceAccountTokenPath(
                "/Users/nameles/Desktop/test_secrets/kubernetes.io/serviceaccount/token_foo");
        kubeEnv.setServiceAccountCaPath("/Users/nameles/Desktop/test_secrets/kubernetes.io/serviceaccount/ca.crt");

        applicationModel.getBeanFactory().registerBean(kubeEnv);
        applicationModel.getBeanFactory().registerBean(new KubeApiClient(applicationModel));

        MapRuleFactory defaultRuleFactory = new MapRuleFactory();
        applicationModel.getBeanFactory().registerBean(defaultRuleFactory);

        KubeRuleProvider provider = new KubeRuleProvider(applicationModel);
        applicationModel.getBeanFactory().registerBean(provider);
        applicationModel.getBeanFactory().registerBean(MapRuleFactory.class);
        //        List<RuleSource> source = provider.getSource(null, null);
        //
        //        List<RuleRoot> rules = defaultRuleFactory.getRules(source.get(0));

        //        HttpBasedMeshRequestCredential credential = new HttpBasedMeshRequestCredential(
        //                "cluster.local/ns/default/sa/sleep",
        //                "test_subject",
        //                "/info",
        //                "GET",
        //                "test",
        //                new HashMap<>()
        //        );
        //
        //        credential.setIssuer("");
        //        credential.setTargetPath();
        //        credential.setServiceName();
        //        credential.setPodId();
        //        credential.setNamespace();
        //        credential.setServiceUid();

        //        AuthorizationRequestContext context = new AuthorizationRequestContext(null,credential);
        //        boolean res = rules.get(0).evaluate(context);
        //
        //        System.out.println(res);
    }

    static {
        System.setProperty(IstioConstant.SERVICE_NAME_KEY, "httpbin");
    }

    static <T> T newRef(ApplicationModel applicationModel, Class<T> serviceClass) {
        ReferenceConfig<T> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setInterface(serviceClass);
        RegistryConfig config = new RegistryConfig("istio://localhost:15012?signer=istio");
        referenceConfig.setRegistry(config);
        referenceConfig.setCluster("xds");
        referenceConfig.setScopeModel(applicationModel.newModule());
        referenceConfig.setTimeout(1000000);
        referenceConfig.getParameters().put("mesh", "istio");
        referenceConfig.getParameters().put("security", "mTLS,serviceIdentity");
        referenceConfig.setProvidedBy("httpbin");
        return referenceConfig.get(false);
    }

    static <T> void newService(ApplicationModel applicationModel, T serviceInst, Class<T> serviceClass, int port) {
        ServiceConfig<T> serviceConfig = new ServiceConfig<>();
        serviceConfig.setRef(serviceInst);
        ProtocolConfig triConf = new ProtocolConfig("tri");
        triConf.setPort(port);
        triConf.setHost("192.168.0.103");
        serviceConfig.setRegistry(new RegistryConfig("istio://localhost:15012?signer=istio"));
        serviceConfig.setProtocol(triConf);
        serviceConfig.setCluster("xds");
        serviceConfig.setScopeModel(applicationModel.newModule());
        serviceConfig.setInterface(serviceClass);
        serviceConfig.setTimeout(1000000);
        serviceConfig.getParameters().put("mesh", "istio");
        serviceConfig.getParameters().put("security", "mTLS,serviceIdentity");
        serviceConfig.export();
    }
}
