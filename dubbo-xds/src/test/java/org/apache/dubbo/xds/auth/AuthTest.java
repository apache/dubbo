package org.apache.dubbo.xds.auth;

import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.xds.kubernetes.KubeApiClient;
import org.apache.dubbo.xds.kubernetes.KubeEnv;
import org.apache.dubbo.xds.security.authz.AuthorizationRequestContext;
import org.apache.dubbo.xds.security.authz.KubeRuleSourceProvider;
import org.apache.dubbo.xds.security.authz.RequestCredential;
import org.apache.dubbo.xds.security.authz.RuleSource;
import org.apache.dubbo.xds.security.authz.rule.DefaultRuleFactory;
import org.apache.dubbo.xds.security.authz.rule.HttpBasedRequestCredential;
import org.apache.dubbo.xds.security.authz.rule.tree.RuleRoot;

import java.util.HashMap;
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

        RequestCredential credential = new HttpBasedRequestCredential(
                "cluster.local/ns/default/sa/sleep",
                "test_subject",
                "/info",
                "GET",
                "test",
                new HashMap<>()
        );

        AuthorizationRequestContext context = new AuthorizationRequestContext(null,credential);
        boolean res = rules.get(0).evaluate(context);

        System.out.println(res);
    }

     static <T> T newRef(FrameworkModel framework,Class<T> serviceClass){
        ReferenceConfig<T> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setInterface(serviceClass);
        referenceConfig.setRegistry(new RegistryConfig("zookeeper://localhost:2181"));
        referenceConfig.setScopeModel(framework.newApplication().newModule());
        referenceConfig.setTimeout(1000000);
        return referenceConfig.get(false);
    }

    static <T> void newService(FrameworkModel framework, T serviceInst, Class<T> serviceClass,int port){
        ServiceConfig<T> serviceConfig =new ServiceConfig<>();
        serviceConfig.setRef(serviceInst);
        ProtocolConfig triConf = new ProtocolConfig("tri");
        triConf.setPort(port);
        triConf.setHost("192.168.0.108");
        serviceConfig.setRegistry(new RegistryConfig("zookeeper://localhost:2181"));
        serviceConfig.setProtocol(triConf);
        serviceConfig.setScopeModel(framework.newApplication().newModule());
        serviceConfig.setInterface(serviceClass);
        serviceConfig.setTimeout(1000000);
        serviceConfig.export();
    }

}
