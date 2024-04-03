package org.apache.dubbo.xds.auth;

import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.xds.kubernetes.KubeApiClient;
import org.apache.dubbo.xds.kubernetes.KubeEnv;
import org.apache.dubbo.xds.security.authz.KubeRuleSourceProvider;
import org.apache.dubbo.xds.security.authz.RuleSource;
import org.apache.dubbo.xds.security.authz.rule.DefaultRuleFactory;
import org.apache.dubbo.xds.security.authz.rule.tree.RuleRoot;

import java.util.List;

import org.junit.Test;

public class AuthTest {


    @Test
    public void authZTest() throws Exception {

        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        KubeEnv kubeEnv = new KubeEnv(applicationModel);
        kubeEnv.setNamespace("foo");
        kubeEnv.setEnableSsl(true);
        kubeEnv.setApiServerPath( "https://127.0.0.1:6443");
        kubeEnv.setServiceAccountTokenPath("/Users/nameles/Desktop/test_secrets/kubernetes.io/serviceaccount/token_foo");
        kubeEnv.setServiceAccountCaPath("/Users/nameles/Desktop/test_secrets/kubernetes.io/serviceaccount/ca.crt");

        applicationModel.getBeanFactory().registerBean(kubeEnv);
        applicationModel.getBeanFactory().registerBean(new KubeApiClient(applicationModel));

        DefaultRuleFactory defaultRuleFactory = new DefaultRuleFactory();
        applicationModel.getBeanFactory().registerBean(defaultRuleFactory);

        KubeRuleSourceProvider provider = new KubeRuleSourceProvider(applicationModel);
        applicationModel.getBeanFactory().registerBean(provider);
        applicationModel.getBeanFactory().registerBean(DefaultRuleFactory.class);
        List<RuleSource> source = provider.getSource(null, null);

        List<RuleRoot> rules = defaultRuleFactory.getRules(source.get(0));

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

     static <T> T newRef(ApplicationModel applicationModel,Class<T> serviceClass){
        ReferenceConfig<T> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setInterface(serviceClass);
        referenceConfig.setRegistry(new RegistryConfig("zookeeper://localhost:2181"));
        referenceConfig.setScopeModel(applicationModel.newModule());
        referenceConfig.setTimeout(1000000);
        referenceConfig.getParameters().put("mesh","istio");
        referenceConfig.getParameters().put("security","mTLS,sa_jwt");
        return referenceConfig.get(false);
    }

    static <T> void newService(ApplicationModel applicationModel, T serviceInst, Class<T> serviceClass,int port){
        ServiceConfig<T> serviceConfig =new ServiceConfig<>();
        serviceConfig.setRef(serviceInst);
        ProtocolConfig triConf = new ProtocolConfig("tri");
        triConf.setPort(port);
        triConf.setHost("192.168.0.103");
        serviceConfig.setRegistry(new RegistryConfig("zookeeper://localhost:2181"));
        serviceConfig.setProtocol(triConf);
        serviceConfig.setScopeModel(applicationModel.newModule());
        serviceConfig.setInterface(serviceClass);
        serviceConfig.setTimeout(1000000);
        serviceConfig.getParameters().put("mesh","istio");
        serviceConfig.getParameters().put("security","mTLS,sa_jwt");
        serviceConfig.export();
    }

}
